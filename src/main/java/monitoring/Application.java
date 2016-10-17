package monitoring;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Application {
    private static final Logger logger = LogManager.getLogger(Application.class);

    public static void main(String[] args) {
        logger.info("Strating application");
        AppInitializer initializer = new AppInitializer();

        try {
            initializer.start(1499);
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
