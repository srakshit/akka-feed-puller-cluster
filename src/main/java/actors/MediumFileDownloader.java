package actors;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import model.feed.Feed;
import model.protocol.WorkComplete;

import java.util.Date;

/**
 * Created by rakshit on 15/03/2018.
 */
public class MediumFileDownloader extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Feed.class, feed -> {
                    String customerFeedName = feed.getCompany() + "-" + feed.getFeedName();
                    log.info("Downloading feed {}", customerFeedName);
                    Thread.sleep(30000);
                    getSender().tell(new WorkComplete(new Date()), getSelf());
                })
                .build();
    }
}
