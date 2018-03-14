package model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Created by rakshit on 13/03/2018.
 */
public abstract class FeedState {
    private final Map<String, Feed> feedDownloadInProgress;
    private final Set<String> acceptedFeeds;
    private final Set<String> downloadedFeeds;
    private final ConcurrentLinkedDeque<Feed> pendingFeeds;

    public FeedState() {
        feedDownloadInProgress = new HashMap<>();
        acceptedFeeds = new HashSet<>();
        downloadedFeeds = new HashSet<>();
        pendingFeeds = new ConcurrentLinkedDeque<>();
    }

    public abstract FeedState updated(IFeedDomainEvent event);

    protected FeedState(FeedState feedState, FeedAccepted feedAccepted){
        ConcurrentLinkedDeque<Feed> newPendingFeeds = new ConcurrentLinkedDeque<>(feedState.pendingFeeds);
        Set<String> newDownloadedFeeds = new HashSet<>(feedState.downloadedFeeds);
        Set<String> newAcceptedFeeds = new HashSet<>(feedState.acceptedFeeds);
        Map<String, Feed> newFeedDownloadInProgress = new HashMap<>(feedState.feedDownloadInProgress);

        if (!newAcceptedFeeds.contains(feedAccepted.feed.getId())) {
            newPendingFeeds.addLast(feedAccepted.feed);
            newAcceptedFeeds.add(feedAccepted.feed.getCompany() + "-" + feedAccepted.feed.getFeedName());
        }

        feedDownloadInProgress = newFeedDownloadInProgress;
        acceptedFeeds = newAcceptedFeeds;
        downloadedFeeds = newDownloadedFeeds;
        pendingFeeds = newPendingFeeds;
    }

    protected FeedState(FeedState feedState, FeedStarted feedStarted){
        ConcurrentLinkedDeque<Feed> newPendingFeeds = new ConcurrentLinkedDeque<>(feedState.pendingFeeds);
        Set<String> newAcceptedFeeds = new HashSet<>(feedState.acceptedFeeds);
        Set<String> newDownloadedFeeds = new HashSet<>(feedState.downloadedFeeds);
        Map<String, Feed> newFeedDownloadInProgress = new HashMap<>(feedState.feedDownloadInProgress);

        Feed feed = newPendingFeeds.removeFirst();
        if (feed.getId() != feedStarted.feed.getId()) {
            throw new IllegalArgumentException("FeedStarted expected feedId "+feed.getId()+"=="+ feedStarted.feed.getId());
        }
        newFeedDownloadInProgress.put(feedStarted.feed.getCompany() + "-" + feedStarted.feed.getFeedName(), feed);

        feedDownloadInProgress = newFeedDownloadInProgress;
        acceptedFeeds = newAcceptedFeeds;
        downloadedFeeds = newDownloadedFeeds;
        pendingFeeds = newPendingFeeds;
    }

    protected FeedState(FeedState feedState, FeedCompleted feedCompleted){
        ConcurrentLinkedDeque<Feed> newPendingFeeds = new ConcurrentLinkedDeque<>(feedState.pendingFeeds);
        Set<String> newAcceptedFeeds = new HashSet<>(feedState.acceptedFeeds);
        Set<String> newDownloadedFeeds = new HashSet<>(feedState.downloadedFeeds);
        Map<String, Feed> newFeedDownloadInProgress = new HashMap<>(feedState.feedDownloadInProgress);

        String customerFeedName = feedCompleted.feed.getCompany() + "-" + feedCompleted.feed.getFeedName();
        newFeedDownloadInProgress.remove(customerFeedName);
        newDownloadedFeeds.add(customerFeedName);
        newAcceptedFeeds.remove(customerFeedName);

        feedDownloadInProgress = newFeedDownloadInProgress;
        acceptedFeeds = newAcceptedFeeds;
        downloadedFeeds = newDownloadedFeeds;
        pendingFeeds = newPendingFeeds;
    }

    protected FeedState(FeedState feedState, FeedFailed feedFailed){
        ConcurrentLinkedDeque<Feed> newPendingFeeds = new ConcurrentLinkedDeque<>(feedState.pendingFeeds);
        Set<String> newAcceptedFeeds = new HashSet<>(feedState.acceptedFeeds);
        Set<String> newDownloadedFeeds = new HashSet<>(feedState.downloadedFeeds);
        Map<String, Feed> newFeedDownloadInProgress = new HashMap<>(feedState.feedDownloadInProgress);

        String customerFeedName = feedFailed.feed.getCompany() + "-" + feedFailed.feed.getFeedName();
        newFeedDownloadInProgress.remove(customerFeedName);
        newAcceptedFeeds.remove(customerFeedName);

        feedDownloadInProgress = newFeedDownloadInProgress;
        acceptedFeeds = newAcceptedFeeds;
        downloadedFeeds = newDownloadedFeeds;
        pendingFeeds = newPendingFeeds;
    }

    public Feed nextFeed() {
        return pendingFeeds.getFirst();
    }

    public boolean hasFeed() {
        return !pendingFeeds.isEmpty();
    }

    public boolean isAccepted(String customerFeedName) {
        return acceptedFeeds.contains(customerFeedName);
    }

    public boolean isDownloadInProgress(String customerFeedName) { return feedDownloadInProgress.containsKey(customerFeedName); }

    public boolean isDownloaded(String customerFeedName) {
        return downloadedFeeds.contains(customerFeedName);
    }
}
