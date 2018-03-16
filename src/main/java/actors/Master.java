package actors;

import akka.actor.AbstractActor;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.client.ClusterClientReceptionist;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import config.AppConfig;
import model.feed.*;
import model.protocol.*;
import model.worker.Busy;
import model.worker.Idle;
import model.worker.WorkerState;
import scala.concurrent.duration.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Master extends AbstractActor {
    public static Props props() {
        return Props.create(Master.class);
    }
    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    Cluster cluster = Cluster.get(getContext().getSystem());

    private List<Feed> feeds;
    private HashMap<String, WorkerState> workers = new HashMap<>();
    private FeedState verySmallFileFeedState = new VerySmallFileFeedState();
    private FeedState smallFileFeedState = new SmallFileFeedState();
    private FeedState mediumFileFeedState = new MediumFileFeedState();
    private FeedState largeFileFeedState = new LargeFileFeedState();
    private Cancellable loadFeedConfigScheduler;
    private Cancellable cleanUpUnRespondingWorkers;

    private static final String CleanupTick = "Tick";

    public Master() {
        ClusterClientReceptionist.get(getContext().system()).registerService(getSelf());
    }

    @Override
    public void preStart() {
        cluster.subscribe(getSelf(), ClusterEvent.initialStateAsEvents(),
                ClusterEvent.MemberEvent.class, ClusterEvent.UnreachableMember.class);
    }

    @Override
    public void postStop() {
        loadFeedConfigScheduler.cancel();
        cleanUpUnRespondingWorkers.cancel();
        cluster.unsubscribe(getSelf());
    }

    //TODO: Test Cleanup task to remove workers every 30 secs
    //TODO: Worker failed and handle backoff
    //TODO: Master event persistence
    //TODO: Dockerise app to demonstrate

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ClusterEvent.MemberUp.class, mUp -> log.info("Member is Up: {}", mUp.member()))
                .match(ClusterEvent.UnreachableMember.class, mUnreachable -> log.info("Member detected as unreachable: {}", mUnreachable.member()))
                .match(ClusterEvent.MemberRemoved.class, mRemoved -> log.info("Member is Removed: {}", mRemoved.member()))
                .match(RegisterWorker.class, worker -> {
                    handleWorkerRegistration(worker);
                    //Schedulers start only when at least one worker is present
                    //as there is no point of getting feeds when there is no worker
                    //- Load job to distribute to worker
                    //- Cleanup un-responding workers
                    startSchedulers();
                })
                .match(WorkerRequestsWork.class, worker -> handleWorkDistribution(worker))
                .match(WorkIsDone.class, worker -> handleWorkComplete(worker))
                .match(WorkResult.class, result -> handleWorkResult(result))
                .match(WorkFailed.class, worker -> handleWorkFailure(worker))
                .match(Work.class, work -> handleWork(work))
                .matchEquals(LoadFeedConfig.class, x -> handleFeedConfigLoad())
                .matchEquals(CleanupTick, x -> handleWorkerListCleanup())
                .build();
    }

    private void handleWorkerRegistration(RegisterWorker worker) {
        FeedState feedState;
        log.debug("Heartbeat of worker {} of type {} is {}",worker.workerId,worker.workerType);

        if (workers.containsKey(worker.workerId))
            workers.put(worker.workerId, workers.get(worker.workerId).copyWithRef(getSender()));
        else
            workers.put(worker.workerId, new WorkerState(getSender(), worker.workerType, Idle.getInstance()));

        feedState = getFeedState(worker.workerType);
        if (feedState.hasFeed()) {
            getSender().tell(WorkIsReady.getInstance(), getSelf());
        }
    }

    private void handleWorkDistribution(WorkerRequestsWork worker) {
        FeedState feedState = getFeedState(worker.workerType);
        if (feedState.hasFeed()) {
            if (workers.containsKey(worker.workerId)) {
                WorkerState state = workers.get(worker.workerId);
                //Only assign work to worker if the worker is idle and
                //available client connection is less than amx concurrent download for the client
                if (state != null && state.status.isIdle()) {
                    Feed feed = feedState.peekFeed();

                    if (checkIfClientSessionsAreAvailable(feed.getCompany())) {
                        feed = feedState.nextFeed();
                        FeedStarted feedStarted = new FeedStarted(feed);
                        feedState = feedState.updated(feedStarted);
                        setFeedState(worker.workerType, feedState);
                        String customerFeedName = feed.getCompany() + "-" + feed.getFeedName();

                        log.info("Giving worker {} to download feed {}", worker.workerId, customerFeedName);
                        //Mark worker as busy
                        workers.put(worker.workerId, workers.get(worker.workerId).copyWithStatus(new Busy(customerFeedName)));

                        //Send Feed to worker to download
                        getSender().tell(feed, getSelf());
                    }else {
                        log.debug("Sit idle {}. Will let you know when download session is available", worker.workerId);
                    }
                }
            } else {
                log.error("Unregistered worker {} can not request feed", worker.workerId);
            }
        } else {
            log.debug("Sit idle {}. Will let you know when feed will come.", worker.workerId);
        }
    }

    private void handleWorkComplete(WorkIsDone worker) {
        FeedState feedState = getFeedState(worker.workerType);
        String customerFeedName = worker.feed.getCompany() + "-" + worker.feed.getFeedName();
        if (feedState.isDownloaded(customerFeedName)) {
            //Send Ack if the previous WorkIsDone request is already served.
            getSender().tell(new Ack(customerFeedName), getSelf());
        } else {
            log.info("Worker {} completed downloaded feed {} at {}", worker.workerId, customerFeedName, worker.lastUpdated);
            if (workers.get(worker.workerId).status.isBusy()) {
                workers.put(worker.workerId, workers.get(worker.workerId).copyWithStatus(Idle.getInstance()));
            }
            FeedCompleted feedCompleted = new FeedCompleted(worker.feed);
            feedState = feedState.updated(feedCompleted);
            setFeedState(worker.workerType, feedState);
            //Send Ack to worker so that they can start working on something else
            getSender().tell(new Ack(customerFeedName), getSelf());
            //Handle WorkResult
            getSelf().tell(new WorkResult(worker.feed, worker.lastUpdated), getSelf());
        }
    }

    private void startSchedulers() {
        if (this.loadFeedConfigScheduler == null) {
            log.info("Started scheduler to retrieve config from JSON every 60 seconds");
            this.loadFeedConfigScheduler = getContext().getSystem().scheduler().schedule(
                    Duration.Zero(),
                    Duration.create(AppConfig.MASTER_LOAD_FEED_INTERVAL, TimeUnit.SECONDS),
                    getSelf(), LoadFeedConfig.class,
                    getContext().dispatcher(),
                    getSelf());
        }
        if (this.cleanUpUnRespondingWorkers == null) {
            log.info("Started scheduler to remove workers that missed heartbeat 3 times");
            this.cleanUpUnRespondingWorkers = getContext().getSystem().scheduler().schedule(
                    Duration.Zero(),
                    Duration.create(AppConfig.MASTER_CLEANUP_SCHEDULE_INTERVAL, TimeUnit.SECONDS),
                    getSelf(), CleanupTick,
                    getContext().dispatcher(),
                    getSelf());
        }
    }

    private void handleWorkResult(WorkResult result) throws IOException {
        log.info("Updating last update time of feed {} in JSON config", result.feed.getCompany() + "-" + result.feed.getFeedName());
        feeds.forEach(feed -> {
            if (feed.getId() == result.feed.getId()) {
                feed.setLastUpdated(result.lastUpdated);
                feed.setOverride(false);
                feed.setBackOff("");
            }
        });

        writeToJson(feeds);
    }

    private void handleWorkFailure(WorkFailed worker) throws IOException {
        FeedState feedState = getFeedState(worker.workerType);
        String customerFeedName = worker.feed.getCompany() + "-" + worker.feed.getFeedName();

        log.info("Worker {} failed to download feed {}", worker.workerId, customerFeedName);

        //Make worker idle
        if (workers.get(worker.workerId).status.isBusy())
            workers.put(worker.workerId, workers.get(worker.workerId).copyWithStatus(Idle.getInstance()));

        FeedFailed feedFailed = new FeedFailed(worker.feed);
        feedState = feedState.updated(feedFailed);
        setFeedState(worker.workerType, feedState);
        //Send Ack to worker so that it can start other work
        getSender().tell(new Ack(customerFeedName), getSelf());

        //Update feed
        log.info("Updating backOff of feed {} in JSON config", customerFeedName);
        feeds.forEach(feed -> {
            if (feed.getId() == feed.getId()) {
                feed.setOverride(false);
                feed.setBackOff("");
            }
        });

        writeToJson(feeds);
    }

    private void handleWork(Work work) {
        for (Feed feed: work.feeds) {
            String customerFeedName = feed.getCompany() + "-" + feed.getFeedName();
            FeedState feedState = getFeedState(feed.getFeedName());

            //If feed is not accepted yet, accept feed
            if (!feedState.isAccepted(customerFeedName)) {
                log.info("Accepted new feeds {}", customerFeedName);
                FeedAccepted feedAccepted = new FeedAccepted(feed);
                feedState = feedState.updated(feedAccepted);
                setFeedState(feed.getFeedName(), feedState);
            }
        }
    }

    private void handleFeedConfigLoad() throws IOException {
        //Read from JSON
        log.info("Loading feed configuration from JSON");
        byte[] jsonData = Files.readAllBytes(Paths.get("feedConfig.json"));
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        feeds = objectMapper.readValue(jsonData, new TypeReference<List<Feed>>() {});

        //Accept feed if difference between present time and lastupdate time is greater than feed interval
        //Accept feed if difference between present time and backoff time is greater than back-off interval
        //Accept feed if feed is overridden, so that it has priority
        List<Feed> filteredFeeds = feeds.stream().filter(
                                        f -> {
                                            try {
                                                return ((new Date().getTime() - f.getLastUpdated().getTime()) >= f.getInterval()) ||
                                                        (f.getBackOff() != "" && (new Date().getTime() - (AppConfig.DATEFORMAT.parse(f.getBackOff())).getTime()) >= AppConfig.BACKOFF_INTERVAL_MILLISECONDS) ||
                                                        f.getOverride();
                                            } catch (ParseException e) {
                                                log.error("Failed to parse backoff datetime {}", f.getBackOff());
                                            }
                                            return false;
                                        })
                                        .collect(Collectors.toList());

        getSender().tell(new Work(filteredFeeds), getSelf());
    }

    private void handleWorkerListCleanup() {
        log.debug("Cleaning up workers that failed to provide heartbeat");
        Iterator it = workers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, WorkerState> worker = (Map.Entry)it.next();
            if (((new Date()).getTime() - worker.getValue().lastHeartBeat.getTime()) >= (AppConfig.WORKER_HEARTBEAT_INTERVAL * 1000 * AppConfig.MAX_HEARTBEAT_MISS_BEFORE_CLEANUP)) {
                log.info("Removing worker {} from list", worker.getKey());
                it.remove();
            }
        }

        if (workers.isEmpty()){
            //Cancel schedules as now worker is present
            loadFeedConfigScheduler.cancel();
            cleanUpUnRespondingWorkers.cancel();
        }
    }

    private void writeToJson(List<Feed> feeds) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
        writer.writeValue(new File("feedConfig.json"), feeds);
    }

    private boolean checkIfClientSessionsAreAvailable(String company) {
        String[] workerType = {"very-small-file", "small-file", "medium-file", "large-file"};
        FeedState feedState;
        int activeClientSession = 0;
        for (String type: workerType) {
            feedState = getFeedState(type);
            if (feedState.isCompanyFeedDownloadInProgress(company))
                activeClientSession += 1;
        }
        return activeClientSession < 2;
    }

    private FeedState getFeedState(String workerType) {
        switch (workerType) {
            case "very-small-file" :
                return verySmallFileFeedState;
            case "small-file" :
                return smallFileFeedState;
            case "medium-file" :
                return mediumFileFeedState;
            case "large-file" :
                return largeFileFeedState;
            default : throw new IllegalArgumentException("Worker Type not found");
        }
    }

    private void setFeedState(String workerType, FeedState feedState) {
        switch (workerType) {
            case "very-small-file" :
                verySmallFileFeedState = feedState;
                break;
            case "small-file" :
                smallFileFeedState = feedState;
                break;
            case "medium-file" :
                mediumFileFeedState = feedState;
                break;
            case "large-file" :
                largeFileFeedState = feedState;
                break;
            default : throw new IllegalArgumentException("Worker Type not found");
        }
    }

    public static final class LoadFeedConfig {}
}