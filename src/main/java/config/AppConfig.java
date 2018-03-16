package config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by rakshit on 16/03/2018.
 */
public class AppConfig {
    private static String configFile = "config.properties";
    public static int NUM_OF_CONCURRENT_CLIENT_SESSION = 2;
    public static String MASTER_PORT_RANGE = "2000:2999";
    public static String WORKER_PORT_RANGE = "3000:3999";
    public static int MASTER_CLEANUP_SCHDULE_INTERVAL = 30;
    public static int MASTER_LOAD_FEED_INTERVAL = 30;
    public static int WORKER_HEARTBEAT_INTERVAL = 10;
    public static int NUM_OF_ACTORS_PER_WORKER = 4;

    public static void loadConfigFromProperties() throws IOException {
        Properties prop = new Properties();
        InputStream input = null;
        try {
            input = new FileInputStream(configFile);
            prop.load(input);
            NUM_OF_CONCURRENT_CLIENT_SESSION = Integer.parseInt(prop.getProperty("num_of_concurrent_client_session"));
            MASTER_PORT_RANGE = prop.getProperty("master_port_range");
            WORKER_PORT_RANGE = prop.getProperty("worker_port_range");
            MASTER_CLEANUP_SCHDULE_INTERVAL = Integer.parseInt(prop.getProperty("master_cleanup_interval_in_seconds"));
            MASTER_LOAD_FEED_INTERVAL = Integer.parseInt(prop.getProperty("master_load_feed_interval_in_seconds"));
            WORKER_HEARTBEAT_INTERVAL = Integer.parseInt(prop.getProperty("worker_heartbeat_interval_in_seconds"));
            NUM_OF_ACTORS_PER_WORKER = Integer.parseInt(prop.getProperty("num_of_actors_per_worker"));
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
