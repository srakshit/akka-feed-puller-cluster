package model;

/**
 * Created by rakshit on 13/03/2018.
 */
public class FeedStarted implements IFeedDomainEvent {
    public Integer feedId;

    public FeedStarted(Integer feedId) {
        this.feedId = feedId;
    }
}
