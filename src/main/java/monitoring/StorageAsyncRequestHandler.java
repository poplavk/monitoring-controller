package monitoring;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.Response;

import java.util.List;

public class StorageAsyncRequestHandler extends AsyncCompletionHandler<Void> {
    private static final Logger logger = LogManager.getLogger(StorageAsyncRequestHandler.class);

    private final List<String> buffer;

    public StorageAsyncRequestHandler(List<String> buffer) {
        this.buffer = buffer;
    }

    @Override
    public Void onCompleted(Response response) throws Exception {
        // basically we are processing response to request for some piece of data,
        // i.e. when indexing service gives us chunks of 10 records, we send every single chunk
        // to storage service and treat chunk as separate request, the only thing that is common among all
        // these chunks is buffer where we append responsed from storage service
        // TODO: 14.10.2016 parse response from storage service
        synchronized (buffer) {
            buffer.add(response.getResponseBody());
        }
        return null;
    }
}
