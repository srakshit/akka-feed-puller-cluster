/**
 * Created by rakshit on 15/03/2018.
 */

import actors.Master;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
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
                "master");
    }
}
