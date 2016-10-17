package monitoring.config;

import com.typesafe.config.Config;

import java.util.concurrent.TimeUnit;

public class Timeouts {
    public final long storageTimeout;
    public final long indexingTimeout;

    public Timeouts(long storageTimeout, long indexingTimeout) {
        this.storageTimeout = storageTimeout;
        this.indexingTimeout = indexingTimeout;
    }

    public Timeouts(Config config) {
        this(config.getDuration("storage-timeout", TimeUnit.MILLISECONDS),
                config.getDuration("indexing-timeout", TimeUnit.MILLISECONDS));
    }

    @Override
    public String toString() {
        return "storage-timeout=" + storageTimeout + "," + "indexing-timeout=" + indexingTimeout;
    }
}
