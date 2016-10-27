package monitoring.offline;

import monitoring.ServerManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.ListenableFuture;
import spark.Request;
import spark.Response;

import java.net.URL;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class OfflineHandler {
    private static final Logger logger = LogManager.getLogger(OfflineHandler.class);

    private ServerManager manager;

    public OfflineHandler(ServerManager manager) {
        this.manager = manager;
    }

    public String handle(String method, Request request, Response response) throws ExecutionException, InterruptedException {
        switch (method) {
            case "/offlineStatus": {
                try {
                    return makeRequest("status");
                } catch (RuntimeException e) {
                    logger.error(e);
                    response.status(500);
                    return "Error: " + e.getMessage();
                }
            }

            case "/offlineStart": {
                try {
                    return makeRequest("start");
                } catch (RuntimeException e) {
                    logger.error(e);
                    response.status(500);
                    return "Error: " + e.getMessage();
                }
            }

            case "/offlineStop": {
                try {
                    return makeRequest("stop");
                } catch (RuntimeException e) {
                    logger.error(e);
                    response.status(500);
                    return "Error: " + e.getMessage();
                }
            }

            case "/offlineNewTask": {
                String task = request.queryParams("task");
                String metric = request.queryParams("metric");
                String from = request.queryParams("from");
                String to = request.queryParams("to");
                String calcStart = request.queryParams("calcStart");
                if (task == null || metric == null || from == null || to == null || calcStart == null) {
                    logger.error("Not enough parameters");
                    response.status(400);
                    return "Not enough parameters";
                }

                try {
                    return makeRequest("task/new?task=" + task + "&" + "metric=" + metric + "&" + "from=" + from + "&" + "to=" + to + "&" + "calcStart=" + calcStart);
                } catch (RuntimeException e) {
                    logger.error(e);
                    response.status(500);
                    return "Error: " + e.getMessage();
                }
            }

            case "/offlineTask/:id/discard": {
                String id = request.params(":id");
                if (id == null) {
                    logger.error("No id parameter in task status request");
                    response.status(400);
                    return "No id parameter";
                }

                try {
                    return makeRequest("task/" + id + "/discard");
                } catch (RuntimeException e) {
                    logger.error(e);
                    response.status(500);
                    return "Error: " + e.getMessage();
                }
            }

            case "/offlineTask/:id/status": {
                String id = request.params(":id");
                if (id == null) {
                    logger.error("No id parameter in task status request");
                    response.status(400);
                    return "No id parameter";
                }

                try {
                    return makeRequest("task/" + id + "/status");
                } catch (RuntimeException e) {
                    logger.error(e);
                    response.status(500);
                    return "Error: " + e.getMessage();
                }
            }

            case "/offlineTask/:id/result": {
                String id = request.params(":id");
                if (id == null) {
                    logger.error("No id parameter in task result request");
                    response.status(400);
                    return "No id parameter";
                }

                try {
                    return makeRequest("task/" + id + "/result");
                } catch (RuntimeException e) {
                    logger.error(e);
                    response.status(500);
                    return "Error: " + e.getMessage();
                }
            }

            default:
                return "Unknown method";
        }
    }

    /**
     *
     * @param urlPath part of URL for exact method
     * @return response string
     */
    private String makeRequest(String urlPath) {
        String url = nextOffline();
        if (url == null)
            throw new RuntimeException("No offline analytics on list");
        else
            url = url + urlPath;

        AsyncHttpClient client = new DefaultAsyncHttpClient();
        logger.debug("URL for requesting offline analytics service: " + url);
        ListenableFuture<String> requestFuture = client.prepareGet(url).execute(new AsyncCompletionHandler<String>() {
            @Override
            public String onCompleted(org.asynchttpclient.Response response) throws Exception {
                return response.getResponseBody(Charset.forName("UTF-8"));
            }
        });

        try {
            return requestFuture.get(5000L, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException("Request timed out");
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    private String nextOffline() {
        URL raw = manager.next();
        if (raw == null) return null;
        return "http://" + raw.getHost() + ":" + raw.getPort() + "/";
    }
}
