package monitoring.config;

import com.typesafe.config.Config;

import java.util.concurrent.TimeUnit;

public class Timeouts {
    public final long storageTimeout;
    public final long indexingTimeout;
    public final long defaultTimeout;

    public Timeouts(long storageTimeout, long indexingTimeout, long defaultTimeout) {
        this.storageTimeout = storageTimeout;
        this.indexingTimeout = indexingTimeout;
        this.defaultTimeout = defaultTimeout;
    }

    public Timeouts(Config config) {
        this(config.getDuration("storage-timeout", TimeUnit.MILLISECONDS),
             config.getDuration("indexing-timeout", TimeUnit.MILLISECONDS),
             config.getDuration("default-timeout", TimeUnit.MILLISECONDS)
        );
    }

    @Override
    public String toString() {
        return "storage-timeout=" + storageTimeout + "," + "indexing-timeout=" + indexingTimeout;
    }
}
