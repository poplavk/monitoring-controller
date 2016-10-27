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
                String online = nextOnline();
                if (online == null) {
                    logger.error("No online on list");
                    response.status(500);
                    return "No online on list";
                } else online = online + "start";

                AsyncHttpClient client = new DefaultAsyncHttpClient();
                logger.debug("URL for requesting online analytics service: " + online);
                ListenableFuture<String> requestFuture = client.prepareGet(online).execute(new AsyncCompletionHandler<String>() {
                    @Override
                    public String onCompleted(org.asynchttpclient.Response response) throws Exception {
                        return response.getResponseBody(Charset.forName("UTF-8"));
                    }
                });

                String responseBody = null;
                try {
                    responseBody = requestFuture.get(5000L, TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    responseBody = "Request timed out";
                    logger.error(response);
                }
                return responseBody;
            }

            case "/onlineStop/:id": {
                String id = request.params(":id");
                if (id == null) {
                    response.status(400);
                    return "Parameter id is not specified";
                }

                String online = nextOnline();
                if (online == null) {
                    logger.error("No online on list");
                    response.status(500);
                    return "No online on list";
                } else online = online + "stop/" + id;

                AsyncHttpClient client = new DefaultAsyncHttpClient();
                logger.debug("URL for requesting indexing service: " + online);
                ListenableFuture<String> requestFuture = client.prepareGet(online).execute(new AsyncCompletionHandler<String>() {
                    @Override
                    public String onCompleted(org.asynchttpclient.Response response) throws Exception {
                        return response.getResponseBody(Charset.forName("UTF-8"));
                    }
                });

                String responseBody = null;
                try {
                    responseBody = requestFuture.get(5000L, TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    responseBody = "Request timed out";
                    logger.error(responseBody);
                }
                return responseBody;
            }

            case "/onlineStatus": {
                String online = nextOnline();
                if (online == null) {
                    logger.error("No online on list");
                    response.status(500);
                    return "No online on list";
                } else online = online + "status";

                AsyncHttpClient client = new DefaultAsyncHttpClient();
                logger.debug("URL for requesting indexing service: " + online);
                ListenableFuture<String> requestFuture = client.prepareGet(online).execute(new AsyncCompletionHandler<String>() {
                    @Override
                    public String onCompleted(org.asynchttpclient.Response response) throws Exception {
                        return response.getResponseBody(Charset.forName("UTF-8"));
                    }
                });

                String responseBody = null;
                try {
                    responseBody = requestFuture.get(5000L, TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    responseBody = "Request timed out";
                    logger.error(responseBody);
                }
                return responseBody;
            }

            case "/onlineStatus/:id": {
                String id = request.params(":id");
                if (id == null) {
                    response.status(400);
                    return "Parameter id is not specified";
                }

                String online = nextOnline();
                if (online == null) {
                    logger.error("No online on list");
                    response.status(500);
                    return "No online on list";
                } else online = online + "status/" + id;

                AsyncHttpClient client = new DefaultAsyncHttpClient();
                logger.debug("URL for requesting indexing service: " + online);
                ListenableFuture<String> requestFuture = client.prepareGet(online).execute(new AsyncCompletionHandler<String>() {
                    @Override
                    public String onCompleted(org.asynchttpclient.Response response) throws Exception {
                        return response.getResponseBody(Charset.forName("UTF-8"));
                    }
                });

                String responseBody = null;
                try {
                    responseBody = requestFuture.get(5000L, TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    responseBody = "Request timed out";
                    logger.error(responseBody);
                }
                return responseBody;
            }

            default:
                return "Unknown method";
        }
    }

    private String nextOnline() {
        URL raw = manager.next();
        if (raw == null) return null;
        return "http://" + raw.getHost() + ":" + raw.getPort() + "/";
    }
}
