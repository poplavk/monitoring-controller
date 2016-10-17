package monitoring.web;

import monitoring.config.Configuration;
import monitoring.indexing.IndexingAsyncRequestHandler;
import monitoring.indexing.IndexingManager;
import monitoring.storage.StorageResponse;
import monitoring.web.request.ClientRequest;
import monitoring.web.request.TimeAndCountRequest;
import monitoring.web.request.TimeRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.ListenableFuture;

import java.net.URL;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class ClientMessageHandler {
    private static final Logger logger = LogManager.getLogger(ClientMessageHandler.class);

    private Configuration config;
    public ClientMessageHandler(Configuration config) { this.config = config; }

    public List<StorageResponse> handle(ClientRequest request) throws InterruptedException, ExecutionException, TimeoutException {
        IndexingAsyncRequestHandler handler = new IndexingAsyncRequestHandler(request);

        AsyncHttpClient client = new DefaultAsyncHttpClient();
        String url = makeIndexURL(request);
        logger.debug("URL for requesting indexing service: " + url);
        ListenableFuture<List<CompletableFuture<StorageResponse>>> requestFuture = client.prepareGet(url).execute(handler);

        List<CompletableFuture<StorageResponse>> storageFuts = requestFuture.get(config.timeouts.indexingTimeout, TimeUnit.MILLISECONDS);
        logger.info("Received " + storageFuts.size() +
                " messages from indexing, sent all to storage, now will wait for storage responses for " + config.timeouts.storageTimeout + " ms");

        CompletableFuture<Void> listFuture = CompletableFuture.allOf(storageFuts.toArray(new CompletableFuture[storageFuts.size()]));
        CompletableFuture<List<StorageResponse>> ff = listFuture.thenApply(v ->
                storageFuts.stream().map(CompletableFuture::join).collect(Collectors.toList())
        );

        return ff.get(config.timeouts.storageTimeout, TimeUnit.MILLISECONDS);
    }

    private String makeIndexURL(ClientRequest clientRequest) {
        URL url = IndexingManager.instance().nextIndexing();
        if (clientRequest instanceof TimeRequest) {
            TimeRequest request = (TimeRequest) clientRequest;
            return "http://" + url.getHost() + ":" + url.getPort() + "/" + "getdata?" + "starttime=" + request.getFrom() + "&" + "endtime=" + request.getTo();
        } else if (clientRequest instanceof TimeAndCountRequest) {
            TimeAndCountRequest request = (TimeAndCountRequest) clientRequest;
            return "http://" + url.getHost() + ":" + url.getPort() + "/" + "getdata?" + "starttime=" + request.getFrom() + "&" + "count=" + request.getCount();
        } else throw new RuntimeException("Unknown type of client request:" + clientRequest);
    }
}
