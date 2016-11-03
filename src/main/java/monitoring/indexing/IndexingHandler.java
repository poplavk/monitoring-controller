package monitoring.indexing;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
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

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static monitoring.utils.ResponseUtils.getError;
import static monitoring.utils.ResponseUtils.getOk;

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
                    return getOk(makeRequest("/getIndexCount/" + timestamp), HttpStatus.OK_200, response, logger);
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
                    return getOk(makeRequest("/getIndexState/" + timestamp), HttpStatus.OK_200, response, logger);
                } catch (RuntimeException e) {
                    return getError("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR_500, response, logger);
                }
            }

            case "/indexKPI": {
                try {
                    return getOk(makeRequest("/getKPI"), HttpStatus.OK_200, response, logger);
                } catch (RuntimeException e) {
                    return getError("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR_500, response, logger);
                }
            }

            case "/indexData/:timestamp": {
                // we dont use makeRequest here, because this logic is much more complex than request-response
                String streamParam = request.queryParams("stream");
                boolean isStream = false;
                if (streamParam == null || !"true".equalsIgnoreCase(streamParam)) {
                    logger.warn("Query parameter 'stream' is not specified at /indexData/:timestamp, will try to get whole response from indexing at once");
                } else {
                    isStream = true;
                    logger.debug("Will use streaming while requesting indexing service");
                }

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

                // if we dont use streaming, try to receive all data at once
                if (!isStream) {
                    logger.debug("Trying to make /indexData/:timestamp request without streaming");
                    try {
                        // make request to indexing service
                        String responseStr = makeRequest(baseUrl + pathUrl);
                        List<String> storageResponses = new ArrayList<>();
                        ObjectMapper mapper = new ObjectMapper();
                        IndexingSyncResponse indexingResponse = mapper.readValue(responseStr, IndexingSyncResponse.class);
                        // for each key in response from indexing service make request to storage
                        for (IndexingResponseChunk chunk : indexingResponse.getKeys()) {
                            try {
                                storageResponses.add(makeStorageKeyRequest(chunk));
                            } catch (RuntimeException e) {
                                return getError("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR_500, response, logger);
                            }
                        }

                        // make response for client
                        String keys = String.join(",", storageResponses);
                        return getOk("{ " +
                                "\"status\": " + "\"" + indexingResponse.getStatus() + "\"" + "," +
                                "\"count\": " + "\"" + indexingResponse.getCount() + "\"" + "," +
                                "\"timestamp\": " + "\"" + indexingResponse.getTimestamp() + "\"" + "," +
                                "\"keys\": " + "[" + keys + "]" +
                                " }", HttpStatus.OK_200, response, logger);
                    } catch (RuntimeException | IOException e) {
                        return getError("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR_500, response, logger);
                    }
                }

                // otherwise we work in streaming mode;
                // prepare handler for processing indexing service response
                logger.debug("URL for requesting indexing service: " + baseUrl + pathUrl);
                IndexingDataRequestHandler handler = new IndexingDataRequestHandler(storageManager);

                // make request to indexing service
                AsyncHttpClient client = new DefaultAsyncHttpClient();
                ListenableFuture<List<CompletableFuture<StorageResponse>>> indexingRequest =
                    client.prepareGet(baseUrl + pathUrl).addHeader("stream", "true").execute(handler);
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
                    return getOk(mapper.writeValueAsString(responses), HttpStatus.OK_200, response, logger);
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

    /**
     * @throws RuntimeException if anything goes wrong
     */
    private String makeStorageKeyRequest(IndexingResponseChunk chunk) {
        URL storage = storageManager.next();
        String storageUrl = "http://" + storage.getHost() + ":" + storage.getPort() + "/key/" + chunk.getKey();
        try {
            return new DefaultAsyncHttpClient().prepareGet(storageUrl).execute(new AsyncCompletionHandler<String>() {
                @Override
                public String onCompleted(org.asynchttpclient.Response response) throws Exception {
                    return response.getResponseBody(Charset.forName("UTF-8"));
                }
            }).get(config.timeouts.storageTimeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
}
