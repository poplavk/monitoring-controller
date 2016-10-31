package monitoring;

import monitoring.config.Configuration;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.ListenableFuture;
import spark.Request;
import spark.Response;

import java.net.URL;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 31.10.16
 */
public abstract class Handler {
    protected ServerManager manager;
    protected Configuration config;
    public abstract String handle(String method, Request request, Response response) throws Exception;

    /** Helper method for making simple request-response operations.
     * @throws RuntimeException if anything goes wrong
     * @param urlPath additional path that will be added to base URL of next indexing service
     * @return result string
     */
    protected String makeRequest(String urlPath) {
        String url = next();
        if (url == null) {
            throw new RuntimeException("No " + manager.getServiceName() + " service on list");
        } else {
            url = url + urlPath;
        }

        AsyncHttpClient client = new DefaultAsyncHttpClient();
        ListenableFuture<String> requestFuture = client.prepareGet(url).execute(new AsyncCompletionHandler<String>() {
            @Override
            public String onCompleted(org.asynchttpclient.Response response) throws Exception {
                return response.getResponseBody(Charset.forName("UTF-8"));
            }
        });

        try {
            return requestFuture.get(config.timeouts.defaultTimeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException("Request timed out");
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    protected String next() {
        URL raw = manager.next();
        if (raw == null) {
            return null;
        }
        return "http://" + raw.getHost() + ":" + raw.getPort() + "/";
    }
}
