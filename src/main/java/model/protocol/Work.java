package model.protocol;

import model.feed.Feed;
import java.io.Serializable;
import java.util.List;

/**
 * Created by rakshit on 16/03/2018.
 */
public final class Work implements Serializable {
    public final List<Feed> feeds;

    public Work(List<Feed> feeds) {
        this.feeds = feeds;
    }

    @Override
    public String toString() {
        return "Work: {feeds=" + feeds +"}";
    }
}
