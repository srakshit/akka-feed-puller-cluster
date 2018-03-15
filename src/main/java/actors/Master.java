package actors;

import akka.actor.AbstractActor;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.Member;
import akka.cluster.client.ClusterClientReceptionist;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.*;
import scala.concurrent.duration.Duration;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Master extends AbstractActor {
    public static Props props() {
        return Props.create(Master.class);
    }
    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    Cluster cluster = Cluster.get(getContext().getSystem());

    private HashMap<String, WorkerState> workers = new HashMap<>();
    private FeedState verySmallFileFeedState = new VerySmallFileFeedState();
    private FeedState smallFileFeedState = new SmallFileFeedState();
    private FeedState mediumFileFeedState = new MediumFileFeedState();
    private FeedState largeFileFeedState = new LargeFileFeedState();
    private Cancellable loadFeedConfigScheduler;

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
        cluster.unsubscribe(getSelf());
    }

    private void notifyWorkers(){
        String[] workerType = {"very-small-file", "small-file", "medium-file", "large-file"};
        FeedState feedState;
        for (String type: workerType) {
            feedState = getFeedState(type);
            if (feedState.hasFeed()) {
                for (WorkerState state: workers.values()) {
                    if (state.type.equalsIgnoreCase(type) && state.status.isIdle()) {
                        log.info("Notifying idle workers about incoming feeds");
                        state.ref.tell(WorkIsReady.instance, getSelf());
                    }
                }
            }
        }
    }
    //Cleanup task to remove workers every 30 secs

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ClusterEvent.MemberUp.class, mUp -> log.info("Member is Up: {}", mUp.member()))
                .match(ClusterEvent.UnreachableMember.class, mUnreachable -> log.info("Member detected as unreachable: {}", mUnreachable.member()))
                .match(ClusterEvent.MemberRemoved.class, mRemoved -> log.info("Member is Removed: {}", mRemoved.member()))
                .match(RegisterWorker.class, worker -> {
                    FeedState feedState;
                    if (workers.containsKey(worker.workerId)) {
                        workers.put(worker.workerId, workers.get(worker.workerId).copyWithRef(getSender()));
                        log.info("Worker Re-Registered : " + worker.toString());
                    } else {
                        workers.put(worker.workerId, new WorkerState(getSender(), worker.workerType, Idle.getInstance()));
                        log.info("Worker Registered : " + worker.toString());
                    }

                    feedState = getFeedState(worker.workerType);
                    if (feedState.hasFeed()) {
                        getSender().tell(WorkIsReady.instance, getSelf());
                    }

                    if (this.loadFeedConfigScheduler == null) {
                        log.info("Started scheduler to retrieve config from JSON every 1 min");
                        this.loadFeedConfigScheduler = getContext().getSystem().scheduler().schedule(
                                Duration.Zero(),
                                Duration.create(1, TimeUnit.MINUTES),
                                getSelf(), LoadFeedConfig.class,
                                getContext().dispatcher(),
                                getSelf());
                    }
                })
                .match(WorkerRequestsWork.class, worker -> {
                    FeedState feedState = getFeedState(worker.workerType);
                    if (feedState.hasFeed()) {
                        if (workers.containsKey(worker.workerId)) {
                            WorkerState state = workers.get(worker.workerId);
                            if (state != null && state.status.isIdle()) {
                                final Feed feed = feedState.nextFeed();
                                FeedStarted feedStarted = new FeedStarted(feed);
                                feedState = feedState.updated(feedStarted);
                                setFeedState(worker.workerType, feedState);
                                String customerFeedName = feed.getCompany() + "-" + feed.getFeedName();
                                log.info("Giving worker {} to download feed {}", worker.workerId, customerFeedName);
                                workers.put(worker.workerId, workers.get(worker.workerId).copyWithStatus(new Busy(customerFeedName)));
                                getSender().tell(feed, getSelf());
                            }
                        } else {
                            log.error("Unregistered worker {} can not request feed", worker.workerId);
                        }
                    } else {
                        log.info("Sit idle {}. Will let you know when feed will come.", worker.workerId);
                    }
                })
                .match(WorkIsDone.class, worker -> {
                    FeedState feedState = getFeedState(worker.workerType);
                    String customerFeedName = worker.feed.getCompany() + "-" + worker.feed.getFeedName();
                    if (feedState.isDownloaded(customerFeedName)) {
                        getSender().tell(new Ack(customerFeedName), getSelf());
                    } else {
                        log.info("Worker {} completed downloaded feed {} at {}", worker.workerId, customerFeedName, worker.result);
                        if (workers.get(worker.workerId).status.isBusy()) {
                            workers.put(worker.workerId, workers.get(worker.workerId).copyWithStatus(Idle.getInstance()));
                        }
                        FeedCompleted feedCompleted = new FeedCompleted(worker.feed);
                        feedState = feedState.updated(feedCompleted);
                        setFeedState(worker.workerType, feedState);
                        getSender().tell(new Ack(customerFeedName), getSelf());
                    }
                })
                .match(WorkFailed.class, worker -> {
                    FeedState feedState = getFeedState(worker.workerType);
                    String customerFeedName = worker.feed.getCompany() + "-" + worker.feed.getFeedName();
                    log.info("Worker {} failed to download feed {}", worker.workerId, customerFeedName);
                    if (workers.get(worker.workerId).status.isBusy()) {
                        workers.put(worker.workerId, workers.get(worker.workerId).copyWithStatus(Idle.getInstance()));
                    }
                    FeedFailed feedFailed = new FeedFailed(worker.feed);
                    feedState = feedState.updated(feedFailed);
                    setFeedState(worker.workerType, feedState);
                    getSender().tell(new Ack(customerFeedName), getSelf());
                })
                .match(Work.class, work -> {
                    for (Feed feed: work.feeds) {
                        String customerFeedName = feed.getCompany() + "-" + feed.getFeedName();
                        FeedState feedState = getFeedState(feed.getFeedName());
                        if (!feedState.isAccepted(customerFeedName)) {
                            log.info("Accepted new feeds {}", customerFeedName);
                            FeedAccepted feedAccepted = new FeedAccepted(feed);
                            feedState = feedState.updated(feedAccepted);
                            setFeedState(feed.getFeedName(), feedState);
                        }
                    }
                })
                .matchEquals(LoadFeedConfig.class, x -> {
                    log.info("Loading feed configuration from JSON");
                    byte[] jsonData = Files.readAllBytes(Paths.get("feedConfig.json"));
                    ObjectMapper objectMapper = new ObjectMapper();
                    objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
                    List<Feed> feeds = objectMapper.readValue(jsonData, new TypeReference<List<Feed>>() {});

                    //for (Feed f: feeds)
                    //    log.info(f.toString());

                    getSender().tell(new Work(feeds), getSelf());
                })
                .build();
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

    public static final class Ack implements Serializable {
        final String feedName;

        public Ack(String feedName) {
            this.feedName = feedName;
        }

        @Override
        public String toString() {
            return "Ack {" + "feedName='" + feedName + "}";
        }
    }

    public static final class Work implements Serializable {
        private final List<Feed> feeds;

        public Work(List<Feed> feeds) {
            this.feeds = feeds;
        }

        @Override
        public String toString() {
            return "Work: {feeds=" + feeds +"}";
        }
    }

    public static final class RegisterWorker implements Serializable {
        private final String workerType;
        private final String workerId;
        private final boolean isIdle;

        public RegisterWorker(String workerType, String workerId, boolean isIdle) {
            this.workerType = workerType;
            this.workerId = workerId;
            this.isIdle = isIdle;
        }

        @Override
        public String toString() {
            return "RegisterWorker: {workerType=" + workerType + ", workerId=" + workerId +", status=" + (isIdle ? "true" : "false") +"}";
        }
    }

    public static final class WorkerRequestsWork implements Serializable {
        private final String workerType;
        private final String workerId;

        public WorkerRequestsWork(String workerType, String workerId) {
            this.workerType = workerType;
            this.workerId = workerId;
        }

        @Override
        public String toString() {
            return "WorkerRequestsWork: {workerType=" + workerType + ", workerId=" + workerId +"}";
        }
    }

    public static final class WorkIsDone implements Serializable {
        private final String workerType;
        private final String workerId;
        private final Feed feed;
        private final String result;

        public WorkIsDone(String workerType, String workerId, Feed feed, String result) {
            this.workerType = workerType;
            this.workerId = workerId;
            this.feed = feed;
            this.result = result;
        }

        @Override
        public String toString() {
            return "WorkIsDone: {workerType=" + workerType + ", workerId=" + workerId + ", feed=" + feed  + ", result=" + result +"}";
        }
    }

    public static final class WorkFailed implements Serializable {
        private final String workerType;
        private final String workerId;
        private final Feed feed;

        public WorkFailed(String workerType, String workerId, Feed feed) {
            this.workerType = workerType;
            this.workerId = workerId;
            this.feed = feed;
        }

        @Override
        public String toString() {
            return "WorkFailed: {workerType=" + workerType + ", workerId=" + workerId + ", feed=" + feed +"}";
        }
    }

    public static final class WorkIsReady implements Serializable {
        private static final WorkIsReady instance = new WorkIsReady();
        public static WorkIsReady getInstance() {
            return instance;
        }
    }
}