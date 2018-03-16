package model.feed;

/**
 * Created by rakshit on 13/03/2018.
 */
public class FeedFailed implements IFeedDomainEvent {
    public Feed feed;

    public FeedFailed(Feed feed) {
        this.feed = feed;
    }
}
