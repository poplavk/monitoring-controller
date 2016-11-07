package monitoring.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.Response;
import org.eclipse.jetty.http.HttpStatus;

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
        if (response.getStatusCode() == HttpStatus.NO_CONTENT_204) {
            logger.error("Value not found: " + response.getUri().toString());
            fut.completeExceptionally(new Exception(
                    "Value not found for URI " + response.getUri().toString()
            ));
        } else if (response.getStatusCode() != HttpStatus.OK_200) {
            logger.error("Error receiving from storage: " + response.toString());
            fut.completeExceptionally(new Exception(
                    "Invalid status code " + response.getStatusCode() +
                    " from server " + response.getRemoteAddress() +
                    ", response body: " + response.getResponseBody())
            );
        } else {
            String rawBody = response.getResponseBody();
            StorageResponse res = mapper.readValue(rawBody, StorageResponse.class);

            logger.info("Received response from storage " + response.getRemoteAddress() + ":"
                    + rawBody + " with status " + response.getStatusCode()
            );
            fut.complete(res);
        }

        return null;
    }
}
