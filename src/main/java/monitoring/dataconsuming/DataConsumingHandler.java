package monitoring.dataconsuming;

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

public class DataConsumingHandler {
    private static final Logger logger = LogManager.getLogger(DataConsumingHandler.class);

    private ServerManager manager;

    public DataConsumingHandler(ServerManager manager) {
        this.manager = manager;
    }

    public String handle(String method, Request request, Response response) throws ExecutionException, InterruptedException {
        switch (method) {
            case "/dataStatus/:id": {
                String id = request.params(":id");
                if (id == null) {
                    response.status(400);
                    return "Parameter id is not specified";
                }

                String url = nextData();
                if (url == null) {
                    logger.error("No data consuming on list");
                    response.status(500);
                    return "No data consuming on list";
                } else url = url + "status/" + id;

                AsyncHttpClient client = new DefaultAsyncHttpClient();
                logger.debug("URL for requesting data consuming service: " + url);
                ListenableFuture<String> requestFuture = client.prepareGet(url).execute(new AsyncCompletionHandler<String>() {
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

            case "/dataStart": {
                String port = request.queryParams("port");
                if (port == null) {
                    logger.error("No port parameter specified");
                    response.status(400);
                    return "No port parameter specified";
                }

                String url = nextData();
                if (url == null) {
                    logger.error("No data consuming on list");
                    response.status(500);
                    return "No data consuming on list";
                } else url = url + "start/?port=" + port;

                AsyncHttpClient client = new DefaultAsyncHttpClient();
                logger.debug("URL for requesting offline analytics service: " + url);
                ListenableFuture<String> requestFuture = client.prepareGet(url).execute(new AsyncCompletionHandler<String>() {
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

            case "/dataStop/:id": {
                String id = request.params(":id");
                if (id == null) {
                    logger.error("No id at stop command");
                    response.status(400);
                    return "No id at stop command";
                }

                String url = nextData();
                if (url == null) {
                    logger.error("No data consuming on list");
                    response.status(500);
                    return "No data consuming on list";
                } else url = url + "stop/" + id;

                AsyncHttpClient client = new DefaultAsyncHttpClient();
                logger.debug("URL for requesting data consuming service: " + url);
                ListenableFuture<String> requestFuture = client.prepareGet(url).execute(new AsyncCompletionHandler<String>() {
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

            case "/dataRestart/:id": {
                String id = request.params(":id");
                if (id == null) {
                    logger.error("No id at stop command");
                    response.status(400);
                    return "No id at stop command";
                }

                String url = nextData();
                if (url == null) {
                    logger.error("No data consuming on list");
                    response.status(500);
                    return "No data consuming on list";
                } else url = url + "restart/" + id;

                AsyncHttpClient client = new DefaultAsyncHttpClient();
                logger.debug("URL for requesting data consuming service: " + url);
                ListenableFuture<String> requestFuture = client.prepareGet(url).execute(new AsyncCompletionHandler<String>() {
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

            case "/dataAllstatus": {
                String url = nextData();
                if (url == null) {
                    logger.error("No data consuming on list");
                    response.status(500);
                    return "No data consuming on list";
                } else url = url + "allstatus";

                AsyncHttpClient client = new DefaultAsyncHttpClient();
                logger.debug("URL for requesting data consuming service: " + url);
                ListenableFuture<String> requestFuture = client.prepareGet(url).execute(new AsyncCompletionHandler<String>() {
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

    private String nextData() {
        URL raw = manager.next();
        if (raw == null) return null;
        return "http://" + raw.getHost() + ":" + raw.getPort() + "/";
    }
}
