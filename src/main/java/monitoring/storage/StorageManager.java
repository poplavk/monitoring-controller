package monitoring.storage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class StorageManager {
    private static final Logger logger = LogManager.getLogger(StorageManager.class);

    private AtomicInteger current = new AtomicInteger(0);
    private final List<URL> storages = new ArrayList<>();

    private static StorageManager instance;

    public static StorageManager instance() {
        if (instance == null) {
            instance = new StorageManager();
        }
        return instance;
    }

    /**
     * @return
     * @throws RuntimeException if storage list is empty
     */
    public URL nextStorage() {
        if (storages.isEmpty()) throw new RuntimeException("No storages in list!");
        int idx = current.incrementAndGet();
        if (idx >= storages.size()) {
            current.set(0);
            idx = 0;
        }
        return storages.get(idx);
    }

    public void add(URL storage) {
        synchronized (storages) {
            if (!storages.contains(storage)) {
                storages.add(storage);
            } else logger.warn("Trying to add storage that is already on list: " + storage);
        }
    }
}
