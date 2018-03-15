/**
 * Created by rakshit on 15/03/2018.
 */

import actors.Master;
import actors.VerySmallFileDownloader;
import actors.Worker;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.cluster.client.ClusterClient;
import akka.cluster.client.ClusterClientSettings;
import akka.cluster.singleton.ClusterSingletonManager;
import akka.cluster.singleton.ClusterSingletonManagerSettings;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class ApplicationMain {
    public static void main(String[] args) throws InterruptedException {
        if (args.length == 0){
            startMaster(2551, "master");
            Thread.sleep(5000);
            startMaster(2552, "master");
            Thread.sleep(5000);
            startWorker(3000, "worker", "very-small-file");
        }
    }

    private static void startMaster(int port, String role) {
        Config conf = ConfigFactory.parseString("akka.cluster.roles=[" + role + "]").
                withFallback(ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port)).
                withFallback(ConfigFactory.load());

        ActorSystem system = ActorSystem.create("ClusterSystem", conf);

        system.actorOf(
                ClusterSingletonManager.props(
                        Master.props(),
                        PoisonPill.getInstance(),
                        ClusterSingletonManagerSettings.create(system).withRole(role)
                ),
                role);
    }

    private static void startWorker(int port, String role, String workerType) {
        Config conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
                withFallback(ConfigFactory.load("worker"));

        ActorSystem system = ActorSystem.create("WorkerSystem", conf);

        ActorRef clusterClient = system.actorOf(ClusterClient.props(ClusterClientSettings.create(system)),"clusterClient");
        for(int i=0; i<2; i++)
            system.actorOf(Worker.props(clusterClient, Props.create(VerySmallFileDownloader.class), workerType), workerType+"-"+role+"-"+(i+1));
    }
}
