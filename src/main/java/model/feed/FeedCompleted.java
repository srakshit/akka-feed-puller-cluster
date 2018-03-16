package model.feed;

/**
 * Created by rakshit on 13/03/2018.
 */
public class FeedCompleted implements IFeedDomainEvent {
    public Feed feed;

    public FeedCompleted(Feed feed) {
        this.feed = feed;
    }
}
