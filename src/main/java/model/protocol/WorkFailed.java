package model.protocol;

import model.feed.Feed;
import java.io.Serializable;

/**
 * Created by rakshit on 16/03/2018.
 */
public final class WorkFailed implements Serializable {
    public final String workerType;
    public final String workerId;
    public final Feed feed;

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
