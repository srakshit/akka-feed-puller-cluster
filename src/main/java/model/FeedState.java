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
    private final Map<String, Integer> activeCompanySessions;
    private final Map<String, Feed> feedDownloadInProgress;
    private final Set<String> acceptedFeeds;
    private final Set<String> downloadedFeeds;
    private final ConcurrentLinkedDeque<Feed> pendingFeeds;

    public FeedState() {
        activeCompanySessions = new HashMap<>();
        feedDownloadInProgress = new HashMap<>();
        acceptedFeeds = new HashSet<>();
        downloadedFeeds = new HashSet<>();
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
        Map<String, Integer> newActiveCompanySessions = new HashMap<>(feedState.activeCompanySessions);
        ConcurrentLinkedDeque<Feed> newPendingFeeds = new ConcurrentLinkedDeque<>(feedState.pendingFeeds);
        Set<String> newDownloadedFeedIds = new HashSet<>(feedState.downloadedFeeds);
        Set<String> newAcceptedFeedIds = new HashSet<>(feedState.acceptedFeeds);
        Map<String, Feed> newFeedDownloadInProgress = new HashMap<>(feedState.feedDownloadInProgress);

        if (!newAcceptedFeedIds.contains(feedAccepted.feed.getId())) {
            newPendingFeeds.addLast(feedAccepted.feed);
            newAcceptedFeedIds.add(feedAccepted.feed.getCompany() + "-" + feedAccepted.feed.getFeedName());
        }

        activeCompanySessions = newActiveCompanySessions;
        feedDownloadInProgress = newFeedDownloadInProgress;
        acceptedFeeds = newAcceptedFeedIds;
        downloadedFeeds = newDownloadedFeedIds;
        pendingFeeds = newPendingFeeds;
    }

    private FeedState(FeedState feedState, FeedStarted feedStarted){
        Map<String, Integer> newActiveCompanySessions = new HashMap<>(feedState.activeCompanySessions);
        ConcurrentLinkedDeque<Feed> newPendingFeeds = new ConcurrentLinkedDeque<>(feedState.pendingFeeds);
        Set<String> newAcceptedFeedIds = new HashSet<>(feedState.acceptedFeeds);
        Set<String> newDownloadedFeedIds = new HashSet<>(feedState.downloadedFeeds);
        Map<String, Feed> newFeedDownloadInProgress = new HashMap<>(feedState.feedDownloadInProgress);

        Feed feed = newPendingFeeds.removeFirst();
        if (feed.getId() != feedStarted.feed.getId()) {
            throw new IllegalArgumentException("FeedStarted expected feedId "+feed.getId()+"=="+ feedStarted.feed.getId());
        }
        newFeedDownloadInProgress.put(feedStarted.feed.getCompany() + "-" + feedStarted.feed.getFeedName(), feed);
        if (newActiveCompanySessions.containsKey(feed.getCompany()))
            newActiveCompanySessions.put(feed.getCompany(), newActiveCompanySessions.get(feed.getCompany()) + 1);
        else
            newActiveCompanySessions.put(feed.getCompany(), 1);

        activeCompanySessions = newActiveCompanySessions;
        feedDownloadInProgress = newFeedDownloadInProgress;
        acceptedFeeds = newAcceptedFeedIds;
        downloadedFeeds = newDownloadedFeedIds;
        pendingFeeds = newPendingFeeds;
    }

    private FeedState(FeedState feedState, FeedCompleted feedCompleted){
        Map<String, Integer> newActiveCompanySessions = new HashMap<>(feedState.activeCompanySessions);
        ConcurrentLinkedDeque<Feed> newPendingFeeds = new ConcurrentLinkedDeque<>(feedState.pendingFeeds);
        Set<String> newAcceptedFeedIds = new HashSet<>(feedState.acceptedFeeds);
        Set<String> newDownloadedFeedIds = new HashSet<>(feedState.downloadedFeeds);
        Map<String, Feed> newFeedDownloadInProgress = new HashMap<>(feedState.feedDownloadInProgress);

        newFeedDownloadInProgress.remove(feedCompleted.feed.getCompany() + "-" + feedCompleted.feed.getFeedName());
        newDownloadedFeedIds.add(feedCompleted.feed.getCompany() + "-" + feedCompleted.feed.getFeedName());
        newActiveCompanySessions.put(feedCompleted.feed.getCompany(), newActiveCompanySessions.get(feedCompleted.feed.getCompany()) - 1);

        activeCompanySessions = newActiveCompanySessions;
        feedDownloadInProgress = newFeedDownloadInProgress;
        acceptedFeeds = newAcceptedFeedIds;
        downloadedFeeds = newDownloadedFeedIds;
        pendingFeeds = newPendingFeeds;
    }

    private FeedState(FeedState feedState, FeedFailed feedFailed){
        Map<String, Integer> newActiveCompanySessions = new HashMap<>(feedState.activeCompanySessions);
        ConcurrentLinkedDeque<Feed> newPendingFeeds = new ConcurrentLinkedDeque<>(feedState.pendingFeeds);
        Set<String> newAcceptedFeedIds = new HashSet<>(feedState.acceptedFeeds);
        Set<String> newDownloadedFeedIds = new HashSet<>(feedState.downloadedFeeds);
        Map<String, Feed> newFeedDownloadInProgress = new HashMap<>(feedState.feedDownloadInProgress);

        newFeedDownloadInProgress.remove(feedFailed.feed.getCompany() + "-" + feedFailed.feed.getFeedName());
        newActiveCompanySessions.put(feedFailed.feed.getCompany(), newActiveCompanySessions.get(feedFailed.feed.getCompany()) - 1);

        activeCompanySessions = newActiveCompanySessions;
        feedDownloadInProgress = newFeedDownloadInProgress;
        acceptedFeeds = newAcceptedFeedIds;
        downloadedFeeds = newDownloadedFeedIds;
        pendingFeeds = newPendingFeeds;
    }

    public Feed nextFeed(String company, String type) {
        Feed newFeed = null;
        if ((activeCompanySessions.get(company) == null || activeCompanySessions.get(company) < 3) && feedDownloadInProgress.get(company + "-" + type) != null) {

        };

        return newFeed;
    }

    public boolean hasFeed() {
        return !pendingFeeds.isEmpty();
    }

    public boolean isAccepted(Integer feedId) {
        return acceptedFeeds.contains(feedId);
    }

    public boolean isDownloadInProgress(Integer feedId) {
        return feedDownloadInProgress.containsKey(feedId);
    }

    public boolean isDownloaded(Integer feedId) {
        return downloadedFeeds.contains(feedId);
    }
}
