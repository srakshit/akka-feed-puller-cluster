package model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Created by rakshit on 13/03/2018.
 */
public final class FeedState {
    private final Map<Integer, Feed> feedDownloadInProgress;
    private final Set<Integer> acceptedFeedIds;
    private final Set<Integer> downloadedFeedIds;
    private final ConcurrentLinkedDeque<Feed> pendingFeeds;

    public FeedState() {
        feedDownloadInProgress = new HashMap<>();
        acceptedFeedIds = new HashSet<>();
        downloadedFeedIds = new HashSet<>();
        pendingFeeds = new ConcurrentLinkedDeque<>();
    }

    public FeedState updated(IFeedDomainEvent event) {
        FeedState newState = null;
        if (event instanceof FeedAccepted)
            newState = new FeedState(this, (FeedAccepted) event);
        if (event instanceof FeedStarted)
            newState = new FeedState(this, (FeedStarted) event);
        if (event instanceof FeedCompleted)
            newState = new FeedState(this, (FeedCompleted) event);
        if (event instanceof FeedFailed)
            newState = new FeedState(this, (FeedFailed) event);
        return newState;
    }

    private FeedState(FeedState feedState, FeedAccepted feedAccepted){
        ConcurrentLinkedDeque<Feed> newPendingFeeds = new ConcurrentLinkedDeque<>(feedState.pendingFeeds);
        Set<Integer> newDownloadedFeedIds = new HashSet<>(feedState.downloadedFeedIds);
        Set<Integer> newAcceptedFeedIds = new HashSet<>(feedState.acceptedFeedIds);
        Map<Integer, Feed> newFeedDownloadInProgress = new HashMap<>(feedState.feedDownloadInProgress);

        if (!newAcceptedFeedIds.contains(feedAccepted.feed.getId())) {
            newPendingFeeds.addLast(feedAccepted.feed);
            newAcceptedFeedIds.add(feedAccepted.feed.getId());
        }

        feedDownloadInProgress = newFeedDownloadInProgress;
        acceptedFeedIds = newAcceptedFeedIds;
        downloadedFeedIds = newDownloadedFeedIds;
        pendingFeeds = newPendingFeeds;
    }

    private FeedState(FeedState feedState, FeedStarted feedStarted){
        ConcurrentLinkedDeque<Feed> newPendingFeeds = new ConcurrentLinkedDeque<>(feedState.pendingFeeds);
        Set<Integer> newAcceptedFeedIds = new HashSet<>(feedState.acceptedFeedIds);
        Set<Integer> newDownloadedFeedIds = new HashSet<>(feedState.downloadedFeedIds);
        Map<Integer, Feed> newFeedDownloadInProgress = new HashMap<>(feedState.feedDownloadInProgress);

        Feed feed = newPendingFeeds.removeFirst();
        if (feed.getId() != feedStarted.feedId) {
            throw new IllegalArgumentException("FeedStarted expected feedId "+feed.getId()+"=="+ feedStarted.feedId);
        }
        newFeedDownloadInProgress.put(feed.getId(), feed);

        feedDownloadInProgress = newFeedDownloadInProgress;
        acceptedFeedIds = newAcceptedFeedIds;
        downloadedFeedIds = newDownloadedFeedIds;
        pendingFeeds = newPendingFeeds;
    }

    private FeedState(FeedState feedState, FeedCompleted feedCompleted){
        ConcurrentLinkedDeque<Feed> newPendingFeeds = new ConcurrentLinkedDeque<>(feedState.pendingFeeds);
        Set<Integer> newAcceptedFeedIds = new HashSet<>(feedState.acceptedFeedIds);
        Set<Integer> newDownloadedFeedIds = new HashSet<>(feedState.downloadedFeedIds);
        Map<Integer, Feed> newFeedDownloadInProgress = new HashMap<>(feedState.feedDownloadInProgress);

        newFeedDownloadInProgress.remove(feedCompleted.feedId);
        newDownloadedFeedIds.add(feedCompleted.feedId);

        feedDownloadInProgress = newFeedDownloadInProgress;
        acceptedFeedIds = newAcceptedFeedIds;
        downloadedFeedIds = newDownloadedFeedIds;
        pendingFeeds = newPendingFeeds;
    }

    private FeedState(FeedState feedState, FeedFailed feedFailed){
        ConcurrentLinkedDeque<Feed> newPendingFeeds = new ConcurrentLinkedDeque<>(feedState.pendingFeeds);
        Set<Integer> newAcceptedFeedIds = new HashSet<>(feedState.acceptedFeedIds);
        Set<Integer> newDownloadedFeedIds = new HashSet<>(feedState.downloadedFeedIds);
        Map<Integer, Feed> newFeedDownloadInProgress = new HashMap<>(feedState.feedDownloadInProgress);

        newFeedDownloadInProgress.remove(feedFailed.feedId);

        feedDownloadInProgress = newFeedDownloadInProgress;
        acceptedFeedIds = newAcceptedFeedIds;
        downloadedFeedIds = newDownloadedFeedIds;
        pendingFeeds = newPendingFeeds;
    }

    public Feed nextFeed() {
        return pendingFeeds.getFirst();
    }

    public boolean hasFeed() {
        return !pendingFeeds.isEmpty();
    }

    public boolean isAccepted(Integer feedId) {
        return acceptedFeedIds.contains(feedId);
    }

    public boolean isDownloadInProgress(Integer feedId) {
        return feedDownloadInProgress.containsKey(feedId);
    }

    public boolean isDownloaded(Integer feedId) {
        return downloadedFeedIds.contains(feedId);
    }
}
