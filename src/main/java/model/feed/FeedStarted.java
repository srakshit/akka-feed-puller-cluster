package model.feed;

/**
 * Created by rakshit on 13/03/2018.
 */
public class FeedStarted implements IFeedDomainEvent {
    public Feed feed;

    public FeedStarted(Feed feed) {
        this.feed = feed;
    }
}
