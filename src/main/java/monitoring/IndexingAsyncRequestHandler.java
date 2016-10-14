package monitoring;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.asynchttpclient.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class IndexingAsyncRequestHandler implements AsyncHandler<Integer> {
    private static final Logger logger = LogManager.getLogger(IndexingAsyncRequestHandler.class);

    private AtomicInteger seq = new AtomicInteger(0);
    private final List<String> buffer;

    public IndexingAsyncRequestHandler(List<String> buffer) {
        this.buffer = buffer;
    }

    @Override
    public void onThrowable(Throwable t) {
        logger.error("error while index", t);
    }

    @Override
    public State onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
        // when we receive a piece of data(chunk) from indexing service, we create separate request to storage
        // service with this piece and handle it as a separate request, the only common thing between chunks is buffer.
        // TODO: 14.10.2016 maybe parse response from indexing service and transform request before sending it to storage?
        seq.incrementAndGet();

        AsyncHttpClient client = new DefaultAsyncHttpClient();
        client.prepareGet("abc").setBody(bodyPart.getBodyPartBytes()).execute(new StorageAsyncRequestHandler(buffer));
        // TODO: 14.10.2016 need to determine target URL of storage service (is it going to be one or more?)
        return State.CONTINUE;
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
    public Integer onCompleted() throws Exception {
        // when we finish receiving responses, we can return count of received messages
        // for this exact request, that were sent to storage service and probably are in-air now.
        // This count can be used by web adapter or whatever to wait until buffer length will equal this count
        return seq.get();
    }
}
