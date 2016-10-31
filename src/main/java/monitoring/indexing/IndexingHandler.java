package monitoring.indexing;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import monitoring.Handler;
import monitoring.ServerManager;
import monitoring.config.Configuration;
import monitoring.storage.StorageResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asynchttpclient.*;
import org.eclipse.jetty.http.HttpStatus;
import spark.Request;
import spark.Response;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static monitoring.utils.ResponseUtils.getError;

public class IndexingHandler extends Handler {
    private static final Logger logger = LogManager.getLogger(IndexingHandler.class);

    private ServerManager storageManager;

    public IndexingHandler(Configuration config, ServerManager indexingManager, ServerManager storageManager) {
        this.config = config;
        this.manager = indexingManager;
        this.storageManager = storageManager;
    }

    public String handle(String method, Request request, Response response) {
        switch (method) {
            case "/indexCount/:timestamp": {
                String timestamp = request.params(":timestamp");
                if (timestamp == null) {
                    return getError("Timestamp not specified", HttpStatus.BAD_REQUEST_400, response, logger);
                }
                try {
                    return makeRequest("/getIndexCount/" + timestamp);
                } catch (RuntimeException e) {
                    return getError("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR_500, response, logger);
                }
            }

            case "/indexState/:timestamp": {
                String timestamp = request.params(":timestamp");
                if (timestamp == null) {
                    return getError("Timestamp not specified", HttpStatus.BAD_REQUEST_400, response, logger);
                }
                try {
                    return makeRequest("/getIndexState/" + timestamp);
                } catch (RuntimeException e) {
                    return getError("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR_500, response, logger);
                }
            }

            case "/indexKPI": {
                try {
                    return makeRequest("/getKPI");
                } catch (RuntimeException e) {
                    return getError("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR_500, response, logger);
                }
            }

            case "/data/:timestamp": {
                // we dont use makeRequest here, because this logic is much more complex than request-response

                String timestamp = request.params(":timestamp");
                if (timestamp == null) {
                    return getError("Timestamp not specified", HttpStatus.BAD_REQUEST_400, response, logger);
                }

                // choose indexing service that we will communicate with
                String baseUrl = next();
                String pathUrl = "getIndexData/" + timestamp;
                if (baseUrl == null) {
                    return getError("No indexing servers are specified",
                                    HttpStatus.INTERNAL_SERVER_ERROR_500, response, logger
                    );
                }

                // prepare handler for processing indexing service response
                logger.debug("URL for requesting indexing service: " + baseUrl + pathUrl);
                IndexingDataRequestHandler handler = new IndexingDataRequestHandler(storageManager);

                // make request to indexing service
                AsyncHttpClient client = new DefaultAsyncHttpClient();
                ListenableFuture<List<CompletableFuture<StorageResponse>>> indexingRequest =
                    client.prepareGet(baseUrl + pathUrl).execute(handler);
                List<CompletableFuture<StorageResponse>> storageResponses;

                // try to wait for indexing service response that we received
                // all keys and sent all of them to storage service
                try {
                    storageResponses = indexingRequest.get(config.timeouts.indexingTimeout, TimeUnit.MILLISECONDS);
                    logger.debug(
                        "Received " + storageResponses.size() + " messages from indexing, sent all to storage"
                    );
                } catch (TimeoutException e) {
                    return getError(
                        "Timeout while requesting indexing sevice: " + e.getMessage(),
                        HttpStatus.INTERNAL_SERVER_ERROR_500, response, logger
                    );
                } catch (InterruptedException | ExecutionException e) {
                    return getError(
                        "Unexpected error while requesting indexing service: " + e.getMessage(),
                        HttpStatus.INTERNAL_SERVER_ERROR_500, response, logger
                    );
                }

                // some magic with futures to transform List<Future> to Future<List>
                CompletableFuture<Void> listFuture = CompletableFuture.allOf(
                    storageResponses.toArray(new CompletableFuture[storageResponses.size()])
                );
                CompletableFuture<List<StorageResponse>> ff = listFuture.thenApply(v ->
                        storageResponses.stream().map(CompletableFuture::join).collect(Collectors.toList())
                );

                try {
                    List<StorageResponse> responses = ff.get(config.timeouts.storageTimeout, TimeUnit.MILLISECONDS);
                    ObjectMapper mapper = new ObjectMapper();
                    return mapper.writeValueAsString(responses);
                } catch (TimeoutException e) {
                    return getError(
                        "Error while waiting for storage service responses: " + e.getMessage(),
                        HttpStatus.INTERNAL_SERVER_ERROR_500, response, logger
                    );
                } catch (ExecutionException | InterruptedException e) {
                    return getError(
                        "Unexpected error while waiting for storage service responses: " + e.getMessage(),
                        HttpStatus.INTERNAL_SERVER_ERROR_500, response, logger
                    );
                } catch (JsonProcessingException e) {
                    return getError(
                        "Serialization exception: " + e.getMessage(),
                        HttpStatus.INTERNAL_SERVER_ERROR_500, response, logger
                    );
                }
            }

            default: {
                return "Unknown method";
            }
        }
    }
}
