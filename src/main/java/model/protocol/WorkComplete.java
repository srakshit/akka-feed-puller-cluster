package model.protocol;

import java.util.Date;

/**
 * Created by rakshit on 16/03/2018.
 */
public class WorkComplete {
    public final Date lastUpdated;

    public WorkComplete(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public String toString() {
        return "WorkComplete: {lastUpdated=" + lastUpdated +"}";
    }
}
