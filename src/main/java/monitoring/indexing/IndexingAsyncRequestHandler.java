package monitoring.indexing;

import com.fasterxml.jackson.databind.ObjectMapper;
import monitoring.storage.StorageAsyncRequestHandler;
import monitoring.storage.StorageManager;
import monitoring.storage.StorageResponse;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.asynchttpclient.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class IndexingAsyncRequestHandler implements AsyncHandler<List<CompletableFuture<StorageResponse>>> {
    private static final Logger logger = LogManager.getLogger(IndexingAsyncRequestHandler.class);

    private URL storage = StorageManager.instance().nextStorage();
    private String storageAddress = storage.getHost() + ":" + storage.getPort();
    private List<CompletableFuture<StorageResponse>> storageFutures = new ArrayList<>();

    private ObjectMapper mapper = new ObjectMapper();

    public IndexingAsyncRequestHandler() {
    }

    @Override
    public void onThrowable(Throwable t) {
        logger.error("error while index", t);
    }

    @Override
    public State onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
        // when we receive a piece of data(chunk) from indexing service, we create separate request to storage
        // service with this piece and handle it as a separate request, the only common thing between chunks is buffer.

        IndexingResponse response = mapper.readValue(bodyPart.getBodyPartBytes(), IndexingResponse.class);

        CompletableFuture<StorageResponse> f = new CompletableFuture<>();
        storageFutures.add(f);

        AsyncHttpClient client = new DefaultAsyncHttpClient();
        client.prepareGet(makeStorageUrl(response)).setBody(makeRequestBody(response)).execute(new StorageAsyncRequestHandler(f));
        return State.CONTINUE;
    }

    private String makeStorageUrl(IndexingResponse indexingResponse) {
        return ""; // todo finish making storage URL
    }

    private String makeRequestBody(IndexingResponse indexingResponse) {
        return ""; // todo finish making storage request body
    }

    @Override
    public State onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
        if (responseStatus.getStatusCode() != 200) {
            logger.error("Status is " + responseStatus.getStatusCode());
            return State.ABORT;
        } else {
            return State.CONTINUE;
        }
    }

    @Override
    public State onHeadersReceived(HttpResponseHeaders headers) throws Exception {
        return State.CONTINUE;
    }

    @Override
    public List<CompletableFuture<StorageResponse>> onCompleted() throws Exception {
        if (storageFutures.isEmpty()) {
            throw new Exception("No body parts");
        }
        return storageFutures;
    }
}
