package actors;

import akka.actor.Props;
import akka.persistence.AbstractPersistentActor;
import model.WorkerState;

import java.util.HashMap;

public class Master extends AbstractPersistentActor {
    private HashMap<String, WorkerState> workers = new HashMap<>();
    //private FeedState workState = new FeedState();

    public static Props props() {
        return Props.create(Master.class);
    }

    @Override
    public Receive createReceiveRecover() {
        return null;
    }

    @Override
    public Receive createReceive() {
        return null;
    }

    @Override
    public String persistenceId() {
        return "feed-master";
    }
}