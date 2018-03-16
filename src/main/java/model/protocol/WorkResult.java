package model.protocol;

import model.feed.Feed;
import java.io.Serializable;
import java.util.Date;

/**
 * Created by rakshit on 16/03/2018.
 */
public final class WorkResult implements Serializable {
    public final Feed feed;
    public final Date lastUpdated;

    public WorkResult(Feed feed, Date lastUpdated) {
        this.feed = feed;
        this.lastUpdated = lastUpdated;
    }

    @Override
    public String toString() {
        return "WorkResult: {feed=" + feed + ", lastUpdated=" + lastUpdated + "}";
    }
}
