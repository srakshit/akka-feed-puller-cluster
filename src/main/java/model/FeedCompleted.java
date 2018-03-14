package model;

/**
 * Created by rakshit on 13/03/2018.
 */
public class FeedCompleted implements IFeedDomainEvent {
    public Integer feedId;

    public FeedCompleted(Integer feedId) {
        this.feedId = feedId;
    }
}
