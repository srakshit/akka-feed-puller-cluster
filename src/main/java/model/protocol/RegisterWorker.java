package model.protocol;

import java.io.Serializable;

/**
 * Created by rakshit on 16/03/2018.
 */
public final class RegisterWorker implements Serializable {
    public final String workerType;
    public final String workerId;

    public RegisterWorker(String workerType, String workerId) {
        this.workerType = workerType;
        this.workerId = workerId;
    }

    @Override
    public String toString() {
        return "HeartBeat: {workerType=" + workerType + ", workerId=" + workerId +"}";
    }
}
