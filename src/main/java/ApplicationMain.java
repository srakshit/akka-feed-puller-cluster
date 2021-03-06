/**
 * Created by rakshit on 15/03/2018.
 */

import actors.*;
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
import config.AppConfig;

import java.util.Arrays;

public class ApplicationMain {
    private static ActorRef clusterClient;
    private static ActorSystem system;

    public static void main(String[] args) throws Exception {
        AppConfig.loadConfigFromProperties();

        if (args.length == 0){
            startMaster(2551, "master");
            Thread.sleep(5000);
            startMaster(2552, "master");
            Thread.sleep(5000);
            startVerySmallFileDownloadWorker(3000, "very-small-file");
            startSmallFileDownloadWorker(3001, "small-file");
            startMediumFileDownloadWorker(3002, "medium-file");
            startLargeFileDownloadWorker(3003, "large-file");
        }else {
            int port = Integer.parseInt(args[0]);
            int[] masterPortArr = Arrays.stream(AppConfig.MASTER_PORT_RANGE.split(":")).mapToInt(x -> Integer.parseInt(x)).toArray();
            int[] workerPortArr = Arrays.stream(AppConfig.WORKER_PORT_RANGE.split(":")).mapToInt(x -> Integer.parseInt(x)).toArray();

            if (masterPortArr[0] <= port && port <= masterPortArr[1])
                startMaster(port, "master");
            else if (workerPortArr[0] <= port && port <= workerPortArr[1]) {
                String workerType = args[1];
                switch(workerType) {
                    case "very-small-file" :
                        startVerySmallFileDownloadWorker(port, workerType);
                        break;
                    case "small-file" :
                        startSmallFileDownloadWorker(port, workerType);
                        break;
                    case "medium-file" :
                        startMediumFileDownloadWorker(port, workerType);
                        break;
                    case "large-file" :
                        startLargeFileDownloadWorker(port, workerType);
                        break;
                    default : throw new IllegalArgumentException("Worker Type not found");
                }
            }
        }
    }

    private static void startMaster(int port, String role) {
        Config conf = ConfigFactory.parseString("akka.cluster.roles=[" + role + "]").
                withFallback(ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port)).
                withFallback(ConfigFactory.load());

        system = ActorSystem.create("ClusterSystem", conf);

        system.actorOf(
                ClusterSingletonManager.props(
                        Master.props(),
                        PoisonPill.getInstance(),
                        ClusterSingletonManagerSettings.create(system).withRole(role)
                ),
                role);
    }

    private static void startVerySmallFileDownloadWorker(int port, String workerType) {
        createActorSystemAndCluster(port, "VerySmallFileDownloadWorker");

        for(int i=0; i<AppConfig.NUM_OF_ACTORS_PER_WORKER; i++) {
            system.actorOf(Worker.props(clusterClient, Props.create(VerySmallFileDownloader.class), workerType), workerType + "-worker-" + (i + 1));
        }
    }

    private static void startSmallFileDownloadWorker(int port, String workerType) {
        createActorSystemAndCluster(port, "SmallFileDownloadWorker");

        for(int i=0; i<AppConfig.NUM_OF_ACTORS_PER_WORKER; i++) {
            system.actorOf(Worker.props(clusterClient, Props.create(SmallFileDownloader.class), workerType), workerType + "-" + (i + 1));
        }
    }

    private static void startMediumFileDownloadWorker(int port, String workerType) {
        createActorSystemAndCluster(port, "MediumFileDownloadWorker");

        for(int i=0; i<AppConfig.NUM_OF_ACTORS_PER_WORKER; i++) {
            system.actorOf(Worker.props(clusterClient, Props.create(MediumFileDownloader.class), workerType), workerType + "-" + (i + 1));
        }
    }

    private static void startLargeFileDownloadWorker(int port, String workerType) {
        createActorSystemAndCluster(port, "LargeFileDownloadWorker");
        for(int i=0; i<AppConfig.NUM_OF_ACTORS_PER_WORKER; i++) {
            system.actorOf(Worker.props(clusterClient, Props.create(LargeFileDownloader.class), workerType), workerType + "-" + (i + 1));
        }
    }

    private static void createActorSystemAndCluster(int port, String workerSystemName){
        Config conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
                withFallback(ConfigFactory.load("worker"));

        system = ActorSystem.create(workerSystemName, conf);

        clusterClient = system.actorOf(ClusterClient.props(ClusterClientSettings.create(system)),"clusterClient");
    }
}
