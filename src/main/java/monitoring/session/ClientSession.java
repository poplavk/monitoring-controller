package monitoring.session;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Deprecated
public class ClientSession {
    public final URL chosenIdxUrl;
    public final URL chosenStorageUrl;
    public final UUID sessionId;
    public final List<String> buffer;
    public final CompletableFuture<Boolean> promise;

    public ClientSession(URL chosenIdxUrl, URL chosenStorageUrl) {
        this.chosenIdxUrl = chosenIdxUrl;
        this.chosenStorageUrl = chosenStorageUrl;
        sessionId = UUID.randomUUID();
        buffer = new ArrayList<>();
        promise = new CompletableFuture<>();
    }
}
