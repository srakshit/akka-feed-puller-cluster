package model;

/**
 * Created by rakshit on 13/03/2018.
 */
public class FeedAccepted implements IFeedDomainEvent {
    public Feed feed;

    public FeedAccepted(Feed feed) {
        this.feed = feed;
    }
}
