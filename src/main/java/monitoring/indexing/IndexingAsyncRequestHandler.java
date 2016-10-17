package monitoring.indexing;

import monitoring.storage.StorageAsyncRequestHandler;
import monitoring.storage.StorageManager;
import monitoring.storage.StorageResponse;
import monitoring.utils.JsonUtils;
import monitoring.web.request.ClientRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asynchttpclient.*;

import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class IndexingAsyncRequestHandler implements AsyncHandler<List<CompletableFuture<StorageResponse>>> {
    private static final Logger logger = LogManager.getLogger(IndexingAsyncRequestHandler.class);

    private List<CompletableFuture<StorageResponse>> storageFutures = new ArrayList<>();

    private ClientRequest clientRequest;

    public IndexingAsyncRequestHandler(ClientRequest request) {
        this.clientRequest = request;
    }

    @Override
    public void onThrowable(Throwable t) {
        logger.error("error while index", t);
    }

    @Override
    public State onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
        String str = new String(bodyPart.getBodyPartBytes(), Charset.forName("UTF-8"));
        logger.debug("String representation: " + str);
        if (!bodyPart.isLast()) {
            List<String> splitted = new ArrayList<>(Arrays.asList(str.split("@")));
            splitted.forEach(s -> {
                IndexingResponse response = JsonUtils.indexingResponse(s);
                logger.info("POJO representation: " + response);

                CompletableFuture<StorageResponse> f = new CompletableFuture<>();
                storageFutures.add(f);

                AsyncHttpClient client = new DefaultAsyncHttpClient();
                String url = makeStorageUrl(response);
                String body = makeRequestBody(response);
                logger.debug("URL for request to storage: " + url);
                logger.debug("Body for request to storage: " + body);
                client.prepareGet(url).setBody(body).execute(new StorageAsyncRequestHandler(f));
            });
        }

        return State.CONTINUE;
    }

    private String makeStorageUrl(IndexingResponse indexingResponse) {
        logger.trace("makeStorageUrl called");
        URL storage = StorageManager.instance().nextStorage();
        String storageAddress = "http://" + storage.getHost() + ":" + storage.getPort() + "/";
        return storageAddress + "test";
    }

    private String makeRequestBody(IndexingResponse indexingResponse) {
        return String.join(",", Arrays.asList(indexingResponse.getData()));
    }

    @Override
    public State onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
        logger.debug("Received status " + responseStatus.getStatusCode() + " from " + responseStatus.getRemoteAddress());
        if (responseStatus.getStatusCode() != 200) {
            logger.error("Status is " + responseStatus.getStatusCode());
            return State.ABORT;
        } else {
            return State.CONTINUE;
        }
    }

    @Override
    public State onHeadersReceived(HttpResponseHeaders headers) throws Exception {
        logger.debug("Received headers " + headers.getHeaders().entries());
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
