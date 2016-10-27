package monitoring;

import monitoring.config.Configuration;
import monitoring.dataconsuming.DataConsumingHandler;
import monitoring.online.OnlineHandler;
import monitoring.storage.StorageResponse;
import monitoring.utils.JsonUtils;
import monitoring.web.ClientMessageHandler;
import monitoring.web.request.ClientRequest;
import monitoring.web.request.TimeAndCountRequest;
import monitoring.web.request.TimeRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asynchttpclient.*;
import spark.Spark;

import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static spark.Spark.get;
import static spark.Spark.port;

public class AppInitializer {
    private static final Logger logger = LogManager.getLogger(AppInitializer.class);

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
        final String fromField = "starttime";
        final String toField = "endtime";
        final String countField = "count";
        final String hostField = "host";
        final String portField = "port";

        get("/getdata", (req, res) -> {
            Map<String, String> params = req.params();
            Set<String> queryParams = req.queryParams();
            logger.info("All parameters: " + params + "," + "query params: " + queryParams);
            String fromParam = req.queryParams(fromField);
            String toParam = req.queryParams(toField);
            String countParam = req.queryParams(countField);

            if (fromParam == null || (toParam == null && countParam == null)) {
                logger.error("Request from " + req.ip() + " does not contain neccesarry parameters");
                res.status(400);
                return "Request does not contain neccessary parameters";
            }
            long from = Long.parseLong(fromParam);

            ClientRequest request = null;

            if (toParam != null) {
                long to = Long.parseLong(toParam);
                request = new TimeRequest(from, to);
            } else if (countParam != null) {
                int count = Integer.parseInt(countParam);
                request = new TimeAndCountRequest(from, count);
            } else {
                logger.error("Unknown request type");
                res.status(404);
                return "Unknown request type";
            }

            ClientMessageHandler handler = new ClientMessageHandler(config, indexingManager, storageManager);

            try {
                List<StorageResponse> responses = handler.handle(request);
                String json = JsonUtils.serialize(responses);
                res.status(200);
                return json;
            } catch (RuntimeException e) {
                logger.error("Error with request", e);
                res.status(500);
                return "Error:" + e;
            } catch (TimeoutException e) {
                logger.error("Timeout exception", e);
                res.status(500);
                return "Timeout exception:" + e;
            }
        });

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
        get("/offlineStatus", (req, res) -> {
            String url = nextOffline();
            if (url == null) {
                logger.error("No offline analytics on list");
                res.status(500);
                return "No offline analytics on list";
            } else url = url + "status";

            AsyncHttpClient client = new DefaultAsyncHttpClient();
            logger.debug("URL for requesting offline analytics service: " + url);
            ListenableFuture<String> requestFuture = client.prepareGet(url).execute(new AsyncCompletionHandler<String>() {
                @Override
                public String onCompleted(Response response) throws Exception {
                    return response.getResponseBody(Charset.forName("UTF-8"));
                }
            });

            String response = null;
            try {
                response = requestFuture.get(5000L, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                response = "Request timed out";
                logger.error(response);
            }
            return response;
        });

        get("/offlineStart", (req, res) -> {
            String url = nextOffline();
            if (url == null) {
                logger.error("No offline analytics on list");
                res.status(500);
                return "No offline analytics on list";
            } else url = url + "start";

            AsyncHttpClient client = new DefaultAsyncHttpClient();
            logger.debug("URL for requesting offline analytics service: " + url);
            ListenableFuture<String> requestFuture = client.prepareGet(url).execute(new AsyncCompletionHandler<String>() {
                @Override
                public String onCompleted(Response response) throws Exception {
                    return response.getResponseBody(Charset.forName("UTF-8"));
                }
            });

            String response = null;
            try {
                response = requestFuture.get(5000L, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                response = "Request timed out";
                logger.error(response);
            }
            return response;
        });

        get("/offlineStop", (req, res) -> {
            String url = nextOffline();
            if (url == null) {
                logger.error("No offline analytics on list");
                res.status(500);
                return "No offline analytics on list";
            } else url = url + "stop";

            AsyncHttpClient client = new DefaultAsyncHttpClient();
            logger.debug("URL for requesting offline analytics service: " + url);
            ListenableFuture<String> requestFuture = client.prepareGet(url).execute(new AsyncCompletionHandler<String>() {
                @Override
                public String onCompleted(Response response) throws Exception {
                    return response.getResponseBody(Charset.forName("UTF-8"));
                }
            });

            String response = null;
            try {
                response = requestFuture.get(5000L, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                response = "Request timed out";
                logger.error(response);
            }
            return response;
        });

        get("/offlineNewTask", (req, res) -> {
            String task = req.queryParams("task");
            String metric = req.queryParams("metric");
            String from = req.queryParams("from");
            String to = req.queryParams("to");
            String calcStart = req.queryParams("calcStart");

            String url = nextOffline();
            if (url == null) {
                logger.error("No offline analytics on list");
                res.status(500);
                return "No offline analytics on list";
            } else
                url = url + "task/new?task=" + task + "&" + "metric=" + metric + "&" + "from=" + from + "&" + "to=" + to + "&" + "calcStart=" + calcStart;

            AsyncHttpClient client = new DefaultAsyncHttpClient();
            logger.debug("URL for requesting offline analytics service: " + url);
            ListenableFuture<String> requestFuture = client.prepareGet(url).execute(new AsyncCompletionHandler<String>() {
                @Override
                public String onCompleted(Response response) throws Exception {
                    return response.getResponseBody(Charset.forName("UTF-8"));
                }
            });

            String response = null;
            try {
                response = requestFuture.get(5000L, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                response = "Request timed out";
                logger.error(response);
            }
            return response;
        });

        get("/offlineTask/:id/discard", (req, res) -> {
            String id = req.params(":id");
            if (id == null) {
                logger.error("No id parameter in task discard request");
                res.status(400);
                return "No id parameter";
            }

            String url = nextOffline();
            if (url == null) {
                logger.error("No offline analytics on list");
                res.status(500);
                return "No offline analytics on list";
            } else url = url + "task/" + id + "/discard";

            AsyncHttpClient client = new DefaultAsyncHttpClient();
            logger.debug("URL for requesting offline analytics service: " + url);
            ListenableFuture<String> requestFuture = client.prepareGet(url).execute(new AsyncCompletionHandler<String>() {
                @Override
                public String onCompleted(Response response) throws Exception {
                    return response.getResponseBody(Charset.forName("UTF-8"));
                }
            });

            String response = null;
            try {
                response = requestFuture.get(5000L, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                response = "Request timed out";
                logger.error(response);
            }
            return response;
        });

        get("/offlineTask/:id/status", (req, res) -> {
            String id = req.params(":id");
            if (id == null) {
                logger.error("No id parameter in task status request");
                res.status(400);
                return "No id parameter";
            }

            String url = nextOffline();
            if (url == null) {
                logger.error("No offline analytics on list");
                res.status(500);
                return "No offline analytics on list";
            } else url = url + "task/" + id + "/status";

            AsyncHttpClient client = new DefaultAsyncHttpClient();
            logger.debug("URL for requesting offline analytics service: " + url);
            ListenableFuture<String> requestFuture = client.prepareGet(url).execute(new AsyncCompletionHandler<String>() {
                @Override
                public String onCompleted(Response response) throws Exception {
                    return response.getResponseBody(Charset.forName("UTF-8"));
                }
            });

            String response = null;
            try {
                response = requestFuture.get(5000L, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                response = "Request timed out";
                logger.error(response);
            }
            return response;
        });

        get("/offlineTask/:id/status", (req, res) -> {
            String id = req.params(":id");
            if (id == null) {
                logger.error("No id parameter in task result request");
                res.status(400);
                return "No id parameter";
            }

            String url = nextOffline();
            if (url == null) {
                logger.error("No offline analytics on list");
                res.status(500);
                return "No offline analytics on list";
            } else url = url + "task/" + id + "/result";

            AsyncHttpClient client = new DefaultAsyncHttpClient();
            logger.debug("URL for requesting offline analytics service: " + url);
            ListenableFuture<String> requestFuture = client.prepareGet(url).execute(new AsyncCompletionHandler<String>() {
                @Override
                public String onCompleted(Response response) throws Exception {
                    return response.getResponseBody(Charset.forName("UTF-8"));
                }
            });

            String response = null;
            try {
                response = requestFuture.get(5000L, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                response = "Request timed out";
                logger.error(response);
            }
            return response;
        });
    }

    private String nextOnline() {
        URL raw = onlineManager.next();
        if (raw == null) return null;
        return "http://" + raw.getHost() + ":" + raw.getPort() + "/";
    }

    private String nextOffline() {
        URL raw = offlineManager.next();
        if (raw == null) return null;
        return "http://" + raw.getHost() + ":" + raw.getPort() + "/";
    }

    private String nextData() {
        URL raw = dataConsumingManager.next();
        if (raw == null) return null;
        return "http://" + raw.getHost() + ":" + raw.getPort() + "/";
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
