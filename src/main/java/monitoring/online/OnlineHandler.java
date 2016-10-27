package monitoring.online;

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

public class OnlineHandler {
    private static final Logger logger = LogManager.getLogger(OnlineHandler.class);

    private ServerManager manager;

    public OnlineHandler(ServerManager manager) {
        this.manager = manager;
    }

    public String handle(String method, Request request, Response response) throws ExecutionException, InterruptedException {
        switch (method) {
            case "/onlineStart": {
                try {
                    return makeRequest("start/");
                } catch (RuntimeException e) {
                    logger.error(e);
                    response.status(500);
                    return "Error: " + e.getMessage();
                }
            }

            case "/onlineStop/:id": {
                String id = request.params(":id");
                if (id == null) {
                    logger.error("id is not specified in online stop");
                    response.status(400);
                    return "Parameter id is not specified";
                }

                try {
                    return makeRequest("stop/" + id);
                } catch (RuntimeException e) {
                    logger.error(e);
                    response.status(500);
                    return "Error: " + e.getMessage();
                }
            }

            case "/onlineStatus": {
                try {
                    return makeRequest("status/");
                } catch (RuntimeException e) {
                    logger.error(e);
                    response.status(500);
                    return "Error: " + e.getMessage();
                }
            }

            case "/onlineStatus/:id": {
                String id = request.params(":id");
                if (id == null) {
                    response.status(400);
                    return "Parameter id is not specified";
                }

                try {
                    return makeRequest("status/" + id);
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

    private String makeRequest(String urlPath) {
        String baseUrl = nextOnline();
        if (baseUrl == null) throw new RuntimeException("No online analytics servers on list");
        else baseUrl = baseUrl + urlPath;

        AsyncHttpClient client = new DefaultAsyncHttpClient();
        logger.debug("URL for requesting indexing service: " + baseUrl);
        ListenableFuture<String> requestFuture = client.prepareGet(baseUrl).execute(new AsyncCompletionHandler<String>() {
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

    private String nextOnline() {
        URL raw = manager.next();
        if (raw == null) return null;
        return "http://" + raw.getHost() + ":" + raw.getPort() + "/";
    }
}
