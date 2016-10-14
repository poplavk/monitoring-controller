package monitoring;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class TestClass {
    private static final Logger logger = LogManager.getLogger(TestClass.class);

    public void testMethod(String url) {
        List<String> buffer = new ArrayList<>();

        AsyncHttpClient client = new DefaultAsyncHttpClient();
        ListenableFuture<Integer> result = client.prepareGet(url).execute(new IndexingAsyncRequestHandler(buffer));
        try {
            int seqLength = result.get(5000L, TimeUnit.MILLISECONDS);
            long timeout = 10000L;
            logger.info("Length of sequences of messages sent to storage service = " + seqLength + ", will wait for " + timeout + " ms");

            long start = System.currentTimeMillis();
            long current = start;
            while (buffer.size() < seqLength && current - start <= timeout) {
                current = System.currentTimeMillis();
            }

            if (current - start > timeout) {
                logger.error("Timeout waiting for storage service responses occured");
            } else logger.info("Buffer contents: " + buffer.stream().collect(Collectors.joining(",")));

        } catch (InterruptedException | ExecutionException e) {
            logger.error("Some unexpected exception occured", e);
        } catch (TimeoutException e) {
            logger.error("Timeout waiting for completion of request to indexing service occured", e);
        }
    }
}
