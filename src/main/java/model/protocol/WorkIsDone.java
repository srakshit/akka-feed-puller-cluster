package model.protocol;

import model.feed.Feed;
import java.io.Serializable;
import java.util.Date;

/**
 * Created by rakshit on 16/03/2018.
 */
public final class WorkIsDone implements Serializable {
    public final String workerType;
    public final String workerId;
    public final Feed feed;
    public final Date lastUpdated;

    public WorkIsDone(String workerType, String workerId, Feed feed, Date lastUpdated) {
        this.workerType = workerType;
        this.workerId = workerId;
        this.feed = feed;
        this.lastUpdated = lastUpdated;
    }

    @Override
    public String toString() {
        return "WorkIsDone: {workerType=" + workerType + ", workerId=" + workerId + ", feed=" + feed  + ", lastUpdated=" + lastUpdated +"}";
    }
}
