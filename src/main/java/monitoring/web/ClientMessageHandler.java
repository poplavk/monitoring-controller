package monitoring.web;

import monitoring.indexing.IndexingAsyncRequestHandler;
import monitoring.storage.StorageResponse;
import monitoring.web.request.ClientRequest;
import monitoring.web.request.TimeAndCountRequest;
import monitoring.web.request.TimeRequest;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.ListenableFuture;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

class ClientMessageHandler {
    private static final Logger logger = LogManager.getLogger(ClientMessageHandler.class);

    List<StorageResponse> handle(ClientRequest request) throws InterruptedException, ExecutionException, TimeoutException {
        String url = "";
        IndexingAsyncRequestHandler handler = new IndexingAsyncRequestHandler();

        // indexing handler returns future that will complete when we will receive all messages from indexing service
        // and send all of them to storage service
        // This future contains list of futures that hold results that storage service must return
        AsyncHttpClient client = new DefaultAsyncHttpClient();
        ListenableFuture<List<CompletableFuture<StorageResponse>>> requestFuture = client.prepareGet(makeIndexURL(request)).execute(handler);

        // wait for all messages to be sent to storage service
        List<CompletableFuture<StorageResponse>> storageFuts = requestFuture.get(5000L, TimeUnit.MILLISECONDS);
        logger.info("Received " + storageFuts.size() + " messages from indexing, sent all to storage, now will wait for storage responses");

        // transform list of futures to future of list so we can stop if any of them fails
        CompletableFuture<Void> listFuture = CompletableFuture.allOf(storageFuts.toArray(new CompletableFuture[storageFuts.size()]));
        CompletableFuture<List<StorageResponse>> ff = listFuture.thenApply(v ->
                storageFuts.stream().map(CompletableFuture::join).collect(Collectors.toList())
        );

        return ff.get(5000L, TimeUnit.MILLISECONDS);
    }

    private String makeIndexURL(ClientRequest clientRequest) {
        if (clientRequest instanceof TimeRequest) {
            TimeRequest request = (TimeRequest) clientRequest;
            return "getdata?" + "starttime=" + request.getFrom() + "&" + "endtime=" + request.getTo();
        } else if (clientRequest instanceof TimeAndCountRequest) {
            TimeAndCountRequest request = (TimeAndCountRequest) clientRequest;
            return "getdata?" + "starttime=" + request.getFrom() + "&" + "count=" + request.getCount();
        } else throw new RuntimeException("Unknown type of client request:" + clientRequest);
    }
}
