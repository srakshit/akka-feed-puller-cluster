package actors;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import actors.Worker.WorkComplete;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by rakshit on 15/03/2018.
 */
public class VerySmallFileDownloader extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Object.class, x -> {
                    log.info("Doing some work!");
                    Thread.sleep(10000);
                    getSender().tell(new WorkComplete(dateFormat.format(new Date())), getSelf());
                })
                .build();
    }
}
