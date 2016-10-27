package monitoring;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Kirill on 27.10.2016.
 */
public class OfflineAnalyticsManager {
    private final static Logger logger = LogManager.getLogger(OfflineAnalyticsManager.class);

    private AtomicInteger current = new AtomicInteger(0);
    private final List<URL> servers = new ArrayList<>();

    private static OfflineAnalyticsManager instance;

    public static OfflineAnalyticsManager instance() {
        if (instance == null) {
            instance = new OfflineAnalyticsManager();
        }
        return instance;
    }

    /**
     * @return
     * @throws RuntimeException if storage list is empty
     */
    public URL next() {
        if (servers.isEmpty()) throw new RuntimeException("No Offline analytics in list!");
        int idx = current.incrementAndGet();
        if (idx >= servers.size()) {
            current.set(0);
            idx = 0;
        }
        return servers.get(idx);
    }

    public void add(URL server) {
        synchronized (this.servers) {
            if (!this.servers.contains(server)) {
                this.servers.add(server);
            } else logger.warn("Trying to add offline analytics server that is already on list: " + server);
        }
    }
}
