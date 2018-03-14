package actors;

import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.persistence.AbstractPersistentActor;
import model.FeedState;
import model.Idle;
import model.WorkerState;

import java.util.HashMap;

public class Master extends AbstractPersistentActor {
    public static Props props() {
        return Props.create(Master.class);
    }
    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    private HashMap<String, WorkerState> workers = new HashMap<>();
    private FeedState feedState = new FeedState();

    @Override
    public Receive createReceiveRecover() {
        return null;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(RegisterWorker.class, worker -> {
                    if (workers.containsKey(worker.workerId)) {
                        workers.put(worker.workerId, workers.get(worker.workerId).copyWithRef(getSender()));
                    } else {
                        workers.put(worker.workerId, new WorkerState(getSender(), Idle.getInstance()));
                        log.info("Worker Registered : " + worker.toString());
                        if (feedState.hasFeed()) {
                            //getSender().tell();
                        }
                    }
                })
                .build();
    }

    @Override
    public String persistenceId() {
        return "feed-master";
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
            return "RegisterWorker: {" + "workerType=" + workerType + "workerId=" + workerId +"}";
        }
    }
}