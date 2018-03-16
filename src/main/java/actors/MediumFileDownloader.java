package actors;

import actors.Worker.WorkComplete;
import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.Date;

/**
 * Created by rakshit on 15/03/2018.
 */
public class MediumFileDownloader extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Object.class, x -> {
                    log.info("Doing some work!");
                    Thread.sleep(10000);
                    getSender().tell(new WorkComplete(new Date()), getSelf());
                })
                .build();
    }
}
