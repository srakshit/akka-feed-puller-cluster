package model.feed;

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
    private final Set<String> companyDownloadInProgress;
    private final ConcurrentLinkedDeque<Feed> pendingFeeds;

    public FeedState() {
        feedDownloadInProgress = new HashMap<>();
        acceptedFeeds = new HashSet<>();
        downloadedFeeds = new HashSet<>();
        companyDownloadInProgress = new HashSet<>();
        pendingFeeds = new ConcurrentLinkedDeque<>();
    }

    public abstract FeedState updated(IFeedDomainEvent event);

    protected FeedState(FeedState feedState, FeedAccepted feedAccepted){
        ConcurrentLinkedDeque<Feed> newPendingFeeds = new ConcurrentLinkedDeque<>(feedState.pendingFeeds);
        Set<String> newDownloadedFeeds = new HashSet<>(feedState.downloadedFeeds);
        Set<String> newAcceptedFeeds = new HashSet<>(feedState.acceptedFeeds);
        Set<String> newCompanyDownloadInProgress = new HashSet<>(feedState.companyDownloadInProgress);
        Map<String, Feed> newFeedDownloadInProgress = new HashMap<>(feedState.feedDownloadInProgress);

        if (!newAcceptedFeeds.contains(feedAccepted.feed.getId())) {
            String customerFeedName = feedAccepted.feed.getCompany() + "-" + feedAccepted.feed.getFeedName();

            newPendingFeeds.addLast(feedAccepted.feed);
            newAcceptedFeeds.add(customerFeedName);
            newDownloadedFeeds.remove(customerFeedName);
        }

        feedDownloadInProgress = newFeedDownloadInProgress;
        companyDownloadInProgress = newCompanyDownloadInProgress;
        acceptedFeeds = newAcceptedFeeds;
        downloadedFeeds = newDownloadedFeeds;
        pendingFeeds = newPendingFeeds;
    }

    protected FeedState(FeedState feedState, FeedStarted feedStarted){
        ConcurrentLinkedDeque<Feed> newPendingFeeds = new ConcurrentLinkedDeque<>(feedState.pendingFeeds);
        Set<String> newAcceptedFeeds = new HashSet<>(feedState.acceptedFeeds);
        Set<String> newDownloadedFeeds = new HashSet<>(feedState.downloadedFeeds);
        Set<String> newCompanyDownloadInProgress = new HashSet<>(feedState.companyDownloadInProgress);
        Map<String, Feed> newFeedDownloadInProgress = new HashMap<>(feedState.feedDownloadInProgress);

        Feed feed = newPendingFeeds.removeFirst();
        if (feed.getId() != feedStarted.feed.getId()) {
            throw new IllegalArgumentException("FeedStarted expected feedId "+feed.getId()+"=="+ feedStarted.feed.getId());
        }
        newFeedDownloadInProgress.put(feedStarted.feed.getCompany() + "-" + feedStarted.feed.getFeedName(), feed);
        newCompanyDownloadInProgress.add(feedStarted.feed.getCompany());

        feedDownloadInProgress = newFeedDownloadInProgress;
        companyDownloadInProgress = newCompanyDownloadInProgress;
        acceptedFeeds = newAcceptedFeeds;
        downloadedFeeds = newDownloadedFeeds;
        pendingFeeds = newPendingFeeds;
    }

    protected FeedState(FeedState feedState, FeedCompleted feedCompleted){
        ConcurrentLinkedDeque<Feed> newPendingFeeds = new ConcurrentLinkedDeque<>(feedState.pendingFeeds);
        Set<String> newAcceptedFeeds = new HashSet<>(feedState.acceptedFeeds);
        Set<String> newDownloadedFeeds = new HashSet<>(feedState.downloadedFeeds);
        Set<String> newCompanyDownloadInProgress = new HashSet<>(feedState.companyDownloadInProgress);
        Map<String, Feed> newFeedDownloadInProgress = new HashMap<>(feedState.feedDownloadInProgress);

        String customerFeedName = feedCompleted.feed.getCompany() + "-" + feedCompleted.feed.getFeedName();
        newFeedDownloadInProgress.remove(customerFeedName);
        newCompanyDownloadInProgress.remove(feedCompleted.feed.getCompany());
        newDownloadedFeeds.add(customerFeedName);
        newAcceptedFeeds.remove(customerFeedName);

        feedDownloadInProgress = newFeedDownloadInProgress;
        companyDownloadInProgress = newCompanyDownloadInProgress;
        acceptedFeeds = newAcceptedFeeds;
        downloadedFeeds = newDownloadedFeeds;
        pendingFeeds = newPendingFeeds;
    }

    protected FeedState(FeedState feedState, FeedFailed feedFailed){
        ConcurrentLinkedDeque<Feed> newPendingFeeds = new ConcurrentLinkedDeque<>(feedState.pendingFeeds);
        Set<String> newAcceptedFeeds = new HashSet<>(feedState.acceptedFeeds);
        Set<String> newDownloadedFeeds = new HashSet<>(feedState.downloadedFeeds);
        Set<String> newCompanyDownloadInProgress = new HashSet<>(feedState.companyDownloadInProgress);
        Map<String, Feed> newFeedDownloadInProgress = new HashMap<>(feedState.feedDownloadInProgress);

        String customerFeedName = feedFailed.feed.getCompany() + "-" + feedFailed.feed.getFeedName();
        newFeedDownloadInProgress.remove(customerFeedName);
        newCompanyDownloadInProgress.remove(feedFailed.feed.getCompany());
        newAcceptedFeeds.remove(customerFeedName);

        feedDownloadInProgress = newFeedDownloadInProgress;
        companyDownloadInProgress = newCompanyDownloadInProgress;
        acceptedFeeds = newAcceptedFeeds;
        downloadedFeeds = newDownloadedFeeds;
        pendingFeeds = newPendingFeeds;
    }

    public Feed nextFeed() { return pendingFeeds.getFirst(); }

    public Feed peekFeed() {
        return pendingFeeds.peekFirst();
    }

    public boolean hasFeed() {
        return !pendingFeeds.isEmpty();
    }

    public boolean isAccepted(String customerFeedName) {
        return acceptedFeeds.contains(customerFeedName);
    }

    public boolean isCompanyFeedDownloadInProgress(String company) { return companyDownloadInProgress.contains(company); }

    public boolean isDownloaded(String customerFeedName) {
        return downloadedFeeds.contains(customerFeedName);
    }
}