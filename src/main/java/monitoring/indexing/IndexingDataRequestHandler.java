package monitoring.indexing;

import monitoring.ServerManager;
import monitoring.storage.StorageAsyncRequestHandler;
import monitoring.storage.StorageResponse;
import monitoring.utils.JsonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asynchttpclient.*;

import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Класс, отвечающий за поточную обработку ответа от сервиса индексации. При получении очередной части ответа,
 * пытается разделить полученную строку по символу @, после чего для каждого полученного элемента делает запрос
 * к сервису хранения данных и добавляет во внутренний список футуру. После того, как получит полностью ответ от сервиса
 * индексации, возвращает список футур, по которым можно получить ответы от сервиса хранения.
 */
public class IndexingDataRequestHandler implements AsyncHandler<List<CompletableFuture<StorageResponse>>> {
    private static final Logger logger = LogManager.getLogger(IndexingDataRequestHandler.class);

    private List<CompletableFuture<StorageResponse>> storageFutures = new ArrayList<>();

    private ServerManager storageManager;

    public IndexingDataRequestHandler(ServerManager storageManager) {
        this.storageManager = storageManager;
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
                IndexingResponseChunk response = JsonUtils.indexingResponse(s);
                logger.info("POJO representation: " + response);
                String url = makeStorageUrl(response);
                if (url == null) {
                    logger.error("No storage on list while receiving part of indexing response");
                } else {
                    logger.debug("URL for request to storage: " + url);

                    CompletableFuture<StorageResponse> f = new CompletableFuture<>();
                    storageFutures.add(f);

                    new DefaultAsyncHttpClient().prepareGet(url).execute(new StorageAsyncRequestHandler(f));
                }
            });
        }

        return State.CONTINUE;
    }

    private String makeStorageUrl(IndexingResponseChunk indexingResponse) {
        logger.trace("makeStorageUrl called");
        URL storage = storageManager.next();
        if (storage == null) {
            return null;
        }
        return "http://" + storage.getHost() + ":" + storage.getPort() + "/key/" + indexingResponse.getKey();
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
        String status = headers.getHeaders().get("status");
        if (status != null && !status.equals("ok")) {
            logger.error("Received not-ok status header from indexing service");
            return State.ABORT;
        }
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
