package monitoring.indexing;


import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class IndexingManager {
    private AtomicInteger current = new AtomicInteger(0);
    private final List<URL> indexing = new ArrayList<>();

    private static IndexingManager instance;

    public static IndexingManager instance() {
        if (instance == null) {
            instance = new IndexingManager();
        }
        return instance;
    }

    /**
     * @return
     * @throws RuntimeException if storage list is empty
     */
    public URL nextIndexing() {
        if (indexing.isEmpty()) throw new RuntimeException("No indexing services in list!");
        int idx = current.incrementAndGet();
        if (idx >= indexing.size()) {
            current.set(0);
            idx = 0;
        }
        return indexing.get(idx);
    }

    public void add(URL indexing) {
        synchronized (this.indexing) {
            if (!this.indexing.contains(indexing)) {
                this.indexing.add(indexing);
            }
        }
    }

}
