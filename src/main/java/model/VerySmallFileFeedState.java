package model;

/**
 * Created by rakshit on 14/03/2018.
 */
public class VerySmallFileFeedState extends FeedState {
    public VerySmallFileFeedState() {
        super();
    }

    @Override
    public FeedState updated(IFeedDomainEvent event) {
        FeedState newState = null;
        if (event instanceof FeedAccepted)
            newState = new VerySmallFileFeedState(this, (FeedAccepted) event);
        if (event instanceof FeedStarted)
            newState = new VerySmallFileFeedState(this, (FeedStarted) event);
        if (event instanceof FeedCompleted)
            newState = new VerySmallFileFeedState(this, (FeedCompleted) event);
        if (event instanceof FeedFailed)
            newState = new VerySmallFileFeedState(this, (FeedFailed) event);
        return newState;
    }

    private VerySmallFileFeedState(FeedState feedState, FeedAccepted feedAccepted){
        super(feedState, feedAccepted);
    }

    private VerySmallFileFeedState(FeedState feedState, FeedStarted feedStarted){
        super(feedState, feedStarted);
    }

    private VerySmallFileFeedState(FeedState feedState, FeedCompleted feedCompleted) { super(feedState, feedCompleted); }

    private VerySmallFileFeedState(FeedState feedState, FeedFailed feedFailed){
        super(feedState, feedFailed);
    }
}
