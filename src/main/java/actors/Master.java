package actors;

import akka.actor.Cancellable;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.persistence.AbstractPersistentActor;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.*;
import scala.concurrent.duration.Duration;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Master extends AbstractPersistentActor {
    public static Props props() {
        return Props.create(Master.class);
    }
    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    private HashMap<String, WorkerState> workers = new HashMap<>();
    private FeedState verySmallFileFeedState = new VerySmallFileFeedState();
    private Cancellable loadFeedConfig;

    public Master() {
        //Move it to cluster worker up event
        this.loadFeedConfig = getContext().getSystem().scheduler().schedule(
                Duration.Zero(),
                Duration.create(1, TimeUnit.MINUTES),
                getSelf(), LoadFeedConfig.class,
                getContext().dispatcher(),
                getSelf());
    }

    @Override
    public Receive createReceiveRecover() {
        return null;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(RegisterWorker.class, worker -> {
                    FeedState feedState;
                    if (workers.containsKey(worker.workerId)) {
                        workers.put(worker.workerId, workers.get(worker.workerId).copyWithRef(getSender()));
                    } else {
                        workers.put(worker.workerId, new WorkerState(getSender(), Idle.getInstance()));
                        log.info("Worker Registered : " + worker.toString());
                        feedState = getFeedState(worker.workerType);
                        if (feedState.hasFeed()) {
                            getSender().tell(WorkIsReady.instance, getSelf());
                        }
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
                            log.error("Unregistered worker {} can not request work", worker.workerId);
                        }
                    } else {
                        log.info("Sit idle {}. Will let you know when work will come.", worker.workerId);
                    }
                })
                .match(WorkIsDone.class, worker -> {
                    FeedState feedState = getFeedState(worker.workerType);
                    String customerFeedName = worker.feed.getCompany() + "-" + worker.feed.getFeedName();
                    if (feedState.isDownloaded(customerFeedName)) {
                        getSender().tell(new Ack(customerFeedName), getSelf());
                    } else {
                        log.info("Feed {} is downloaded by worker {}", customerFeedName, worker.workerId);
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
                    log.info("Feed {} has failed to download by worker {}", customerFeedName, worker.workerId);
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
                .match(LoadFeedConfig.class, x -> {
                    log.info("Loading feed configuration from JSON");
                    byte[] jsonData = Files.readAllBytes(Paths.get("feedConfig.json"));
                    ObjectMapper objectMapper = new ObjectMapper();
                    objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
                    List<Feed> feeds = objectMapper.readValue(jsonData, new TypeReference<List<Feed>>() {
                    });

                    for (Feed f: feeds)
                        System.out.println(f);
                    System.out.println();

                    getSender().tell(new Work(feeds), getSelf());
                })
                .build();
    }

    private FeedState getFeedState(String workerType) {
        switch (workerType) {
            case "very-small-file" :
                return verySmallFileFeedState;
            default : throw new IllegalArgumentException("Worker Type not found");
        }
    }

    private void setFeedState(String workerType, FeedState feedState) {
        switch (workerType) {
            case "very-small-file" :
                verySmallFileFeedState = feedState;
                break;
            default : throw new IllegalArgumentException("Worker Type not found");
        }
    }

    @Override
    public String persistenceId() {
        return "feed-master";
    }

    public static final class LoadFeedConfig {
        @Override
        public String toString() {
            return "LoadFeedConfig";
        }
    }

    public static final class Ack {
        final String feedName;

        public Ack(String feedName) {
            this.feedName = feedName;
        }

        @Override
        public String toString() {
            return "Ack {" + "feedName='" + feedName + "}";
        }
    }

    public static final class Work {
        private final List<Feed> feeds;

        public Work(List<Feed> feeds) {
            this.feeds = feeds;
        }

        @Override
        public String toString() {
            return "Work: {feeds=" + feeds +"}";
        }
    }

    public static final class RegisterWorker {
        private final String workerType;
        private final String workerId;

        public RegisterWorker(String workerType, String workerId) {
            this.workerType = workerType;
            this.workerId = workerId;
        }

        @Override
        public String toString() {
            return "RegisterWorker: {workerType=" + workerType + "workerId=" + workerId +"}";
        }
    }

    public static final class WorkerRequestsWork {
        private final String workerType;
        private final String workerId;

        public WorkerRequestsWork(String workerType, String workerId) {
            this.workerType = workerType;
            this.workerId = workerId;
        }

        @Override
        public String toString() {
            return "WorkerRequestsWork: {workerType=" + workerType + "workerId=" + workerId +"}";
        }
    }

    public static final class WorkIsDone {
        private final String workerType;
        private final String workerId;
        private final Feed feed;

        public WorkIsDone(String workerType, String workerId, Feed feed) {
            this.workerType = workerType;
            this.workerId = workerId;
            this.feed = feed;
        }

        @Override
        public String toString() {
            return "WorkIsDone: {workerType=" + workerType + "workerId=" + workerId + "feed=" + feed +"}";
        }
    }

    public static final class WorkFailed {
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
            return "WorkFailed: {workerType=" + workerType + "workerId=" + workerId + "feed=" + feed +"}";
        }
    }

    public static final class WorkIsReady {
        private static final WorkIsReady instance = new WorkIsReady();
        public static WorkIsReady getInstance() {
            return instance;
        }
    }
}