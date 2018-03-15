package model;

/**
 * Created by rakshit on 14/03/2018.
 */
public class SmallFileFeedState extends FeedState {
    public SmallFileFeedState() {
        super();
    }

    @Override
    public FeedState updated(IFeedDomainEvent event) {
        FeedState newState = null;
        if (event instanceof FeedAccepted)
            newState = new SmallFileFeedState(this, (FeedAccepted) event);
        if (event instanceof FeedStarted)
            newState = new SmallFileFeedState(this, (FeedStarted) event);
        if (event instanceof FeedCompleted)
            newState = new SmallFileFeedState(this, (FeedCompleted) event);
        if (event instanceof FeedFailed)
            newState = new SmallFileFeedState(this, (FeedFailed) event);
        return newState;
    }

    private SmallFileFeedState(FeedState feedState, FeedAccepted feedAccepted){
        super(feedState, feedAccepted);
    }

    private SmallFileFeedState(FeedState feedState, FeedStarted feedStarted){
        super(feedState, feedStarted);
    }

    private SmallFileFeedState(FeedState feedState, FeedCompleted feedCompleted) { super(feedState, feedCompleted); }

    private SmallFileFeedState(FeedState feedState, FeedFailed feedFailed){
        super(feedState, feedFailed);
    }
}
