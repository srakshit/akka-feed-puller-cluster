package actors;

import akka.actor.*;
import akka.cluster.client.ClusterClient;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import model.Feed;
import scala.concurrent.duration.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static akka.actor.SupervisorStrategy.escalate;
import static akka.actor.SupervisorStrategy.restart;
import static akka.actor.SupervisorStrategy.stop;

/**
 * Created by rakshit on 15/03/2018.
 */
public class Worker extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    public static final Props props(ActorRef clusterClient, Props workExecutorProps, String workerType) {
        return Props.create(Worker.class, clusterClient, workExecutorProps, workerType);
    }

    private final ActorRef clusterClient;
    private final String workerId = UUID.randomUUID().toString();
    private final ActorRef workExecutor;
    private final Cancellable registerTask;
    private Feed currentFeed = null;
    private String workerType = null;

    public Worker(ActorRef clusterClient, Props workExecutorProps, String workerType) {
        this.clusterClient = clusterClient;
        this.workExecutor = getContext().watch(getContext().actorOf(workExecutorProps, "executor"));
        this.workerType = workerType;
        this.registerTask = getContext().system().scheduler().schedule(
                                Duration.Zero(),
                                Duration.create(10, TimeUnit.SECONDS),
                                clusterClient,
                                new ClusterClient.SendToAll("/user/master/singleton", new Master.RegisterWorker(workerType, workerId)),
                                getContext().dispatcher(),
                                getSelf());
    }

    private Feed getCurrentFeed(){
        if (currentFeed != null)
            return currentFeed;
        throw new IllegalStateException("Not working on any feed");
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return new OneForOneStrategy(-1, Duration.Inf(),
                t -> {
                    if (t instanceof ActorInitializationException)
                        return stop();
                    else if (t instanceof DeathPactException)
                        return stop();
                    else if (t instanceof Exception) {
                        if (currentFeed != null)
                            sendToMaster(new Master.WorkFailed(workerType, workerId, getCurrentFeed()));
                        //getContext().become(idle);
                        return restart();
                    }
                    else {
                        return escalate();
                    }
                }
        );
    }

    @Override
    public void postStop() {
        registerTask.cancel();
    }

    private void sendToMaster(Object msg) {
        clusterClient.tell(new ClusterClient.SendToAll("/user/master/singleton", msg), getSelf());
    }

    @Override
    public Receive createReceive() {
        return null;
    }
}
