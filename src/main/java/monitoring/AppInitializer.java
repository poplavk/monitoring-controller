package monitoring;

import monitoring.config.Configuration;
import monitoring.dataconsuming.DataConsumingHandler;
import monitoring.indexing.IndexingHandler;
import monitoring.offline.OfflineHandler;
import monitoring.online.OnlineHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spark.Spark;

import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;

import static spark.Spark.get;
import static spark.Spark.port;

public class AppInitializer {
    private static final Logger logger = LogManager.getLogger(AppInitializer.class);

    final String fromField = "starttime";
    final String toField = "endtime";
    final String countField = "count";
    final String hostField = "host";
    final String portField = "port";

    private Configuration config;
    private ServerManager dataConsumingManager = new ServerManager("data consuming");
    private ServerManager onlineManager = new ServerManager("online analytics");
    private ServerManager offlineManager = new ServerManager("offline analytics");
    private ServerManager storageManager = new ServerManager("storage service");
    private ServerManager indexingManager = new ServerManager("indexing");

    public AppInitializer(Configuration config) {
        this.config = config;
    }

    public void start() {
        port(config.port);
        createRoutes();
        setup();

        logger.info("Server successfully started at port: " + config.port);
    }

    private void createRoutes() {
        get("/addIndexing", (req, res) -> {
            String host = req.queryParams(hostField);
            String port = req.queryParams(portField);
            if (host == null) {
                String error = "No " + hostField + " parameter";
                logger.error(error);
                res.status(400);
                return error;
            } else if (port == null) {
                String error = "No " + portField + " parameter";
                logger.error(error);
                res.status(400);
                return error;
            } else {
                logger.debug("Received request to add indexing service with host " + host + ":" + port);
                try {
                    addToManager(indexingManager, host, Integer.parseInt(port));
                    String response = "Indexing service at URL " + host + ":" + port + " successfully added";
                    logger.info(response);
                    res.status(200);
                    return response;
                } catch (UnknownHostException e) {
                    String error = "Unknown host at " + host + ":" + port;
                    logger.error(error, e);
                    res.status(500);
                    return error;
                } catch (IOException e) {
                    logger.error("Error at adding host", e);
                    String error = "Error adding host " + host + ":" + port + ", exception: " + e;
                    res.status(500);
                    return error;
                }
            }
        });

        get("/addStorage", (req, res) -> {
            String host = req.queryParams(hostField);
            String port = req.queryParams(portField);
            if (host == null) {
                String error = "No " + hostField + " parameter";
                logger.error(error);
                res.status(500);
                return error;
            } else if (port == null) {
                String error = "No " + portField + " parameter";
                logger.error(error);
                res.status(500);
                return error;
            } else {
                logger.debug("Received request to add storage with host " + host + ":" + port);
                try {
                    addToManager(storageManager, host, Integer.parseInt(port));
                    String response = "Storage service at URL " + host + ":" + port + " successfully added";
                    logger.info(response);
                    res.status(200);
                    return response;
                } catch (IOException e) {
                    logger.error("Error at adding host", e);
                    String error = "Error adding host " + host + ":" + port + ", exception: " + e;
                    res.status(500);
                    return error;
                }
            }
        });

        /** =========== INDEXING METHODS ===================**/
        IndexingHandler handler = new IndexingHandler(config, indexingManager, storageManager);

        get("/data/:timestamp", (req, res) -> handler.handle("/data/:timestamp", req, res));
        get("/indexCount/:timestamp", (req, res) -> handler.handle("/indexCount/:timestamp", req, res));
        get("/indexState/:timestamp", (req, res) -> handler.handle("/indexState/:timestamp", req, res));
        get("/indexKPI", (req, res) -> handler.handle("/indexKPI", req, res));

        /** =========== END INDEXING METHODS ===================**/

        /** =========== ONLINE ANALYTICS METHODS ===================**/
        OnlineHandler onlineHandler = new OnlineHandler(onlineManager);

        get("/onlineStart", (req, res) -> onlineHandler.handle("/onlineStart", req, res));
        get("/onlineStop/:id", (req, res) -> onlineHandler.handle("/onlineStop/:id", req, res));
        get("/onlineStatus", (req, res) -> onlineHandler.handle("/onlineStatus", req, res));
        get("/onlineStatus/:id", (req, res) -> onlineHandler.handle("/onlineStatus/:id", req, res));

        /** =========== END ONLINE ANALYTICS METHODS ===================**/


        /** =========== DATA CONSUMING METHODS ===================**/
        DataConsumingHandler dataConsumingHandler = new DataConsumingHandler(dataConsumingManager);

        get("/dataStatus/:id", (req, res) -> dataConsumingHandler.handle("/dataStatus/:id", req, res));
        get("/dataStart", (req, res) -> dataConsumingHandler.handle("/dataStart", req, res));
        get("/dataStop/:id", (req, res) -> dataConsumingHandler.handle("/dataStop/:id", req, res));
        get("/dataRestart/:id", (req, res) -> dataConsumingHandler.handle("/dataRestart/:id", req, res));
        get("/dataAllstatus", (req, res) -> dataConsumingHandler.handle("/dataAllstatus", req, res));

        /** =========== END DATA CONSUMING METHODS ===================**/


        /** =========== OFFLINE ANALYTICS METHODS ===================**/
        OfflineHandler offlineHandler = new OfflineHandler(offlineManager);

        get("/offlineStatus", (req, res) -> offlineHandler.handle("/offlineStatus", req, res));
        get("/offlineStart", (req, res) -> offlineHandler.handle("/offlineStart", req, res));
        get("/offlineStop", (req, res) -> offlineHandler.handle("/offlineStop", req, res));
        get("/offlineNewTask", (req, res) -> offlineHandler.handle("/offlineNewTask", req, res));
        get("/offlineTask/:id/discard", (req, res) -> offlineHandler.handle("/offlineTask/:id/discard", req, res));
        get("/offlineTask/:id/status", (req, res) -> offlineHandler.handle("/offlineTask/:id/status", req, res));
        get("/offlineTask/:id/result", (req, res) -> offlineHandler.handle("/offlineTask/:id/result", req, res));

        /** =========== END OFFLINE ANALYTICS METHODS ===================**/
    }

    private void setup() {
        config.storages.forEach(s -> {
            String host = s.substring(0, s.indexOf(":"));
            int port = Integer.parseInt(s.substring(s.indexOf(":") + 1));
            try {
                addToManager(storageManager, host, port);
            } catch (IOException e) {
                logger.error("Error at initial setup of storage @ " + host + ":" + port);
            }
        });

        config.indexes.forEach(s -> {
            String host = s.substring(0, s.indexOf(":"));
            int port = Integer.parseInt(s.substring(s.indexOf(":") + 1));
            try {
                addToManager(indexingManager, host, port);
            } catch (IOException e) {
                logger.error("Error at initial setup of indexing @ " + host + ":" + port);
            }
        });
    }

    private void addToManager(ServerManager manager, String host, int port) throws IOException {
        Socket sock = new Socket(host, port);
        sock.close();

        URL url = new URL("http://" + host + ":" + port + "/");
        manager.add(url);
    }

    public void stop() {
        logger.info("Shutting down...");
        Spark.stop();
        System.exit(0);
    }
}
