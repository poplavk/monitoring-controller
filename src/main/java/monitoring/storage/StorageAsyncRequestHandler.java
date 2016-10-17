package monitoring.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.Response;

import java.util.concurrent.CompletableFuture;

public class StorageAsyncRequestHandler extends AsyncCompletionHandler<Void> {
    private static final Logger logger = LogManager.getLogger(StorageAsyncRequestHandler.class);

    private CompletableFuture<StorageResponse> fut;
    private ObjectMapper mapper = new ObjectMapper();

    public StorageAsyncRequestHandler(CompletableFuture<StorageResponse> fut) {
        this.fut = fut;
    }

    @Override
    public Void onCompleted(Response response) throws Exception {
        // basically we are processing response to request for some piece of data,
        // i.e. when indexing service gives us chunks of 10 records, we send every single chunk
        // to storage service and treat chunk as separate request
        // When we receive response for this request, we mark Future as complete
        // TODO: 14.10.2016 parse response from storage service
        if (response.getStatusCode() != 200) {
            fut.completeExceptionally(new Exception("Invalid status code " + response.getStatusCode() +
                    " from server " + response.getRemoteAddress() +
                    ", response body: " + response.getResponseBody()));
            return null;
        }

        String rawBody = response.getResponseBody();
        StorageResponse res = mapper.readValue(rawBody, StorageResponse.class);

        logger.info("Received response from storage " + response.getRemoteAddress() + ":" + rawBody + " with status " + response.getStatusCode());
        fut.complete(res);

        return null;
    }
}
