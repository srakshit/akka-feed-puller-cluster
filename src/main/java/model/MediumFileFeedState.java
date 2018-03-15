package model;

/**
 * Created by rakshit on 14/03/2018.
 */
public class MediumFileFeedState extends FeedState {
    public MediumFileFeedState() {
        super();
    }

    @Override
    public FeedState updated(IFeedDomainEvent event) {
        FeedState newState = null;
        if (event instanceof FeedAccepted)
            newState = new MediumFileFeedState(this, (FeedAccepted) event);
        if (event instanceof FeedStarted)
            newState = new MediumFileFeedState(this, (FeedStarted) event);
        if (event instanceof FeedCompleted)
            newState = new MediumFileFeedState(this, (FeedCompleted) event);
        if (event instanceof FeedFailed)
            newState = new MediumFileFeedState(this, (FeedFailed) event);
        return newState;
    }

    private MediumFileFeedState(FeedState feedState, FeedAccepted feedAccepted){
        super(feedState, feedAccepted);
    }

    private MediumFileFeedState(FeedState feedState, FeedStarted feedStarted){
        super(feedState, feedStarted);
    }

    private MediumFileFeedState(FeedState feedState, FeedCompleted feedCompleted) { super(feedState, feedCompleted); }

    private MediumFileFeedState(FeedState feedState, FeedFailed feedFailed){
        super(feedState, feedFailed);
    }
}
