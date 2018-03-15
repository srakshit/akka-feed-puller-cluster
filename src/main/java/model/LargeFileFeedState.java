package model;

/**
 * Created by rakshit on 14/03/2018.
 */
public class LargeFileFeedState extends FeedState {
    public LargeFileFeedState() {
        super();
    }

    @Override
    public FeedState updated(IFeedDomainEvent event) {
        FeedState newState = null;
        if (event instanceof FeedAccepted)
            newState = new LargeFileFeedState(this, (FeedAccepted) event);
        if (event instanceof FeedStarted)
            newState = new LargeFileFeedState(this, (FeedStarted) event);
        if (event instanceof FeedCompleted)
            newState = new LargeFileFeedState(this, (FeedCompleted) event);
        if (event instanceof FeedFailed)
            newState = new LargeFileFeedState(this, (FeedFailed) event);
        return newState;
    }

    private LargeFileFeedState(FeedState feedState, FeedAccepted feedAccepted){
        super(feedState, feedAccepted);
    }

    private LargeFileFeedState(FeedState feedState, FeedStarted feedStarted){
        super(feedState, feedStarted);
    }

    private LargeFileFeedState(FeedState feedState, FeedCompleted feedCompleted) { super(feedState, feedCompleted); }

    private LargeFileFeedState(FeedState feedState, FeedFailed feedFailed){
        super(feedState, feedFailed);
    }
}
