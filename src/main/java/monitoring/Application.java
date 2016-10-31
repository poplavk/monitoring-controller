package monitoring;


import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import monitoring.config.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

public class Application {
    private static final Logger logger = LogManager.getLogger(Application.class);

    private Application() { }

    public static void main(String[] args) {
        logger.info("Starting application");

        final String appConfigPath = "config/application.conf";

        Config root = ConfigFactory.parseFile(new File(appConfigPath));
        Configuration config = new Configuration(root.getConfig("monitoring-controller"));
        logger.info("Loaded configuration file from " + appConfigPath + ":\n" + config.toString());

        AppInitializer initializer = new AppInitializer(config);

        try {
            initializer.start();
            logger.info("Application started successfully");
        } catch (Throwable t) {
            logger.error("Error starting application", t);
            System.exit(1);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            initializer.stop();
        }));
    }
}
