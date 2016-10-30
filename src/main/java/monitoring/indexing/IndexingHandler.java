package monitoring.indexing;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import monitoring.ServerManager;
import monitoring.config.Configuration;
import monitoring.storage.StorageResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asynchttpclient.*;
import spark.Request;
import spark.Response;

import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class IndexingHandler {
    private static final Logger logger = LogManager.getLogger(IndexingHandler.class);

    private Configuration config;
    private ServerManager indexingManager;
    private ServerManager storageManager;

    public IndexingHandler(Configuration config, ServerManager indexingManager, ServerManager storageManager) {
        this.config = config;
        this.indexingManager = indexingManager;
        this.storageManager = storageManager;
    }

    public String handle(String method, Request request, Response response) {
        switch (method) {
            case "/indexCount/:timestamp": {
                String timestamp = request.params(":timestamp");
                if (timestamp == null) {
                    response.status(400);
                    return "Timestamp not specified";
                }
                try {
                    return makeRequest("/getIndexCount/" + timestamp);
                } catch (RuntimeException e) {
                    logger.error("Error while executing " + method, e);
                    response.status(500);
                    return "Error: " + e.getMessage();
                }
            }

            case "/indexState/:timestamp": {
                String timestamp = request.params(":timestamp");
                if (timestamp == null) {
                    response.status(400);
                    return "Timestamp not specified";
                }

                try {
                    return makeRequest("/getIndexState/" + timestamp);
                } catch (RuntimeException e) {
                    logger.error("Error while executing " + method, e);
                    response.status(500);
                    return "Error: " + e.getMessage();
                }
            }

            case "/indexKPI": {
                try {
                    return makeRequest("/getKPI");
                } catch (RuntimeException e) {
                    logger.error("Error while executing " + method, e);
                    response.status(500);
                    return "Error: " + e.getMessage();
                }
            }

            case "/data/:timestamp": {
                // we dont use makeRequest here, because this logic is much more complex than request-response

                String timestamp = request.params(":timestamp");
                if (timestamp == null) {
                    response.status(400);
                    return "Timestamp not specified";
                }

                // choose indexing service that we will communicate with
                String baseUrl = nextIndexing();
                String pathUrl = "getIndexData/" + timestamp;
                if (baseUrl == null) {
                    response.status(500);
                    return "No indexing servers are specified";
                }

                // prepare handler for processing indexing service response
                logger.debug("URL for requesting indexing service: " + baseUrl + pathUrl);
                IndexingDataRequestHandler handler = new IndexingDataRequestHandler(storageManager);

                // make request to indexing service
                AsyncHttpClient client = new DefaultAsyncHttpClient();
                ListenableFuture<List<CompletableFuture<StorageResponse>>> indexingRequest = client.prepareGet(baseUrl + pathUrl).execute(handler);
                List<CompletableFuture<StorageResponse>> storageResponses;

                // try to wait for indexing service response that we received all keys and sent all of them to storage service
                try {
                    storageResponses = indexingRequest.get(config.timeouts.indexingTimeout, TimeUnit.MILLISECONDS);
                    logger.debug("Received " + storageResponses.size() + " messages from indexing, sent all to storage");
                } catch (TimeoutException e) {
                    logger.error("Error while waiting for indexing service to respond", e);
                    response.status(500);
                    return "Timeout while requesting indexing sevice: " + e.getMessage();
                } catch (InterruptedException | ExecutionException e) {
                    logger.error("Unexpected error while requesting indexing service", e);
                    response.status(500);
                    return "Unexpected error while requesting indexing service: " + e.getMessage();
                }

                // some magic with futures to transform List<Future> to Future<List>
                CompletableFuture<Void> listFuture = CompletableFuture.allOf(storageResponses.toArray(new CompletableFuture[storageResponses.size()]));
                CompletableFuture<List<StorageResponse>> ff = listFuture.thenApply(v ->
                        storageResponses.stream().map(CompletableFuture::join).collect(Collectors.toList())
                );

                try {
                    List<StorageResponse> responses = ff.get(config.timeouts.storageTimeout, TimeUnit.MILLISECONDS);
                    ObjectMapper mapper = new ObjectMapper();
                    return mapper.writeValueAsString(responses);
                } catch (TimeoutException e) {
                    logger.error("Timeout error while waiting for storage service responses", e);
                    response.status(500);
                    return "Error while waiting for storage service responses: " + e.getMessage();
                } catch (ExecutionException | InterruptedException e) {
                    logger.error("Unexpected error while waiting for storage service responses", e);
                    response.status(500);
                    return "Unexpected error while waiting for storage service responses: " + e.getMessage();
                } catch (JsonProcessingException e) {
                    logger.error("Serialization exception", e);
                    response.status(500);
                    return "Serialization exception: " + e.getMessage();
                }
            }

            default: {
                return "Unknown method";
            }
        }
    }

    /** Helper method for making simple request-response operations
     * @throws RuntimeException if anything goes wrong
     * @param urlPath additional path that will be added to base URL of next indexing service
     * @return result string
     */
    private String makeRequest(String urlPath) {
        String url = nextIndexing();
        if (url == null)
            throw new RuntimeException("No indexing services on list");
        else
            url = url + urlPath;

        AsyncHttpClient client = new DefaultAsyncHttpClient();
        logger.debug("URL for requesting indexing service: " + url);
        ListenableFuture<String> requestFuture = client.prepareGet(url).execute(new AsyncCompletionHandler<String>() {
            @Override
            public String onCompleted(org.asynchttpclient.Response response) throws Exception {
                return response.getResponseBody(Charset.forName("UTF-8"));
            }
        });

        try {
            return requestFuture.get(5000L, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException("Request timed out", e);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    private String nextIndexing() {
        URL raw = indexingManager.next();
        if (raw == null) return null;
        return "http://" + raw.getHost() + ":" + raw.getPort() + "/";
    }
}
