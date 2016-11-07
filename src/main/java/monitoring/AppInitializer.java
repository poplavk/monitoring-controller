package monitoring;

import monitoring.config.Configuration;
import monitoring.dataconsuming.DataConsumingHandler;
import monitoring.indexing.IndexingHandler;
import monitoring.offline.OfflineHandler;
import monitoring.online.OnlineHandler;
import monitoring.storage.StorageHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpStatus;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.io.IOException;
import java.net.Socket;
import java.net.URL;

import static monitoring.utils.ResponseUtils.getError;
import static monitoring.utils.ResponseUtils.getOk;
import static spark.Spark.*;

public class AppInitializer {
    private static final Logger logger = LogManager.getLogger(AppInitializer.class);

    final String hostField = "host";
    final String portField = "port";

    private Configuration config;

    private MetricsInfoTable table = new MetricsInfoTable();
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
        /** =========== METRICS INFO METHODS ===================**/
        MetricsInfoHandler infoHandler = new MetricsInfoHandler(table, config);

        get("/getMetricsTable", (req, res) -> infoHandler.handle("/getMetricsTable", req, res));
        post("/startMonitoring", (req, res) -> infoHandler.handle("/startMonitoring", req, res));
        delete("/stopMonitoring", (req, res) -> infoHandler.handle("/stopMonitoring", req, res));

        /** =========== END METRICS INFO METHODS ===================**/


        /** =========== INDEXING METHODS ===================**/
        IndexingHandler handler = new IndexingHandler(config, table, indexingManager, storageManager);

        get("/getMetrics", (req, res) -> handler.handle("/getMetrics", req, res));
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

        /** =========== STORAGE METHODS ===================**/
        StorageHandler storageHandler = new StorageHandler(storageManager);

        get("/storageData", (req, res) -> storageHandler.handle("/storageData", req, res));
        get("/storageGetByKey/:key", (req, res) -> storageHandler.handle("/storageGetByKey/:key", req, res));

        /** =========== END STORAGE METHODS ===================**/

        /** =========== ADD METHODS ===================**/
        post("/addIndexing", (req, res) -> addNode(indexingManager, req, res));
        post("/addStorage", (req, res) -> addNode(storageManager, req, res));
        post("/addOffline", (req, res) -> addNode(offlineManager, req, res));
        post("/addOnline", (req, res) -> addNode(onlineManager, req, res));
        post("/addDataConsumer", (req, res) -> addNode(dataConsumingManager, req, res));
        /** =========== END ADD METHODS ===================**/
    }

    private String addNode(ServerManager manager, Request request, Response response) {
        String host = request.queryParams(hostField);
        String port = request.queryParams(portField);
        if (host == null) {
            return getError("No " + hostField + " parameter", HttpStatus.INTERNAL_SERVER_ERROR_500, response, logger);
        } else if (port == null) {
            return getError("No " + portField + " parameter", HttpStatus.INTERNAL_SERVER_ERROR_500, response, logger);
        } else {
            logger.debug("Received request to add " + manager.getServiceName() + " with host " + host + ":" + port);
            try {
                addToManager(manager, host, Integer.parseInt(port));
                return getOk(
                        manager.getServiceName() + " at URL " + host + ":" + port + " successfully added",
                        HttpStatus.OK_200, response, logger
                );
            } catch (IOException e) {
                return getError(
                        "Error adding host " + host + ":" + port + ", exception: " + e,
                        HttpStatus.INTERNAL_SERVER_ERROR_500, response, logger
                );
            }
        }
    }



    private void setup() {
        config.storages.forEach(s -> {
            parseAndAddToManager(s, storageManager);
        });
        config.indexes.forEach(s -> {
            parseAndAddToManager(s, indexingManager);
        });
    }

    private void parseAndAddToManager(String s, ServerManager manager) {
        String host = s.substring(0, s.indexOf(":"));
        int port = Integer.parseInt(s.substring(s.indexOf(":") + 1));
        try {
            addToManager(manager, host, port);
        } catch (IOException e) {
            logger.error("Error at initial setup of service @ " + host + ":" + port);
        }
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
