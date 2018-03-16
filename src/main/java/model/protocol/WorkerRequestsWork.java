package model.protocol;

import java.io.Serializable;

/**
 * Created by rakshit on 16/03/2018.
 */
public final class WorkerRequestsWork implements Serializable {
    public final String workerType;
    public final String workerId;

    public WorkerRequestsWork(String workerType, String workerId) {
        this.workerType = workerType;
        this.workerId = workerId;
    }

    @Override
    public String toString() {
        return "WorkerRequestsWork: {workerType=" + workerType + ", workerId=" + workerId +"}";
    }
}
