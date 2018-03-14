package model;

/**
 * Created by rakshit on 13/03/2018.
 */
public class FeedFailed implements IFeedDomainEvent {
    public Integer feedId;

    public FeedFailed(Integer feedId) {
        this.feedId = feedId;
    }
}
