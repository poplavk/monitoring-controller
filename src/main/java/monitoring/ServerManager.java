package monitoring;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerManager {
    private final static Logger logger = LogManager.getLogger(ServerManager.class);
    private final String serviceName;

    private AtomicInteger current = new AtomicInteger(0);
    private final List<URL> servers = new ArrayList<>();

    public ServerManager(String serviceName) {
        this.serviceName = serviceName;
    }

    public URL next() {
        if (servers.isEmpty()) return null;
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
            } else logger.warn("Trying to add " + serviceName + " server that is already on list: " + server);
        }
    }
}
