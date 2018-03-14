package model;

import akka.actor.ActorRef;

/**
 * Created by rakshit on 13/03/2018.
 */
public final class WorkerState {
    public final ActorRef ref;
    public final WorkerStatus status;

    public WorkerState(ActorRef ref, WorkerStatus status) {
        this.ref = ref;
        this.status = status;
    }

    public WorkerState copyWithRef(ActorRef ref) {
        return new WorkerState(ref, this.status);
    }

    public WorkerState copyWithStatus(WorkerStatus status) {
        return new WorkerState(this.ref, status);
    }
}