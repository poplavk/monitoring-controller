package monitoring.config;


import com.typesafe.config.Config;

import java.util.List;

public class Configuration {
    public final int port;
    public final List<String> supportedMetricTypes;
    public final List<String> storages;
    public final List<String> indexes;

    public final Timeouts timeouts;

    public final int maxResultAmount;

    public Configuration(int port, List<String> supportedMetricTypes, List<String> storages,
                         List<String> indexes, Timeouts timeouts, int maxResultAmount) {
        this.port = port;
        this.supportedMetricTypes = supportedMetricTypes;
        this.storages = storages;
        this.indexes = indexes;
        this.timeouts = timeouts;
        this.maxResultAmount = maxResultAmount;
    }

    public Configuration(Config config) {
        this(config.getInt("port"),
                config.getStringList("supportedTypes"),
                config.getStringList("storages"),
                config.getStringList("indexes"),
                new Timeouts(config.getConfig("network")),
                config.getInt("maxResultAmount")
        );
    }

    @Override
    public String toString() {
        return "Configuration:\n" +
                "\tsupported metric types: [" + String.join(",", supportedMetricTypes) + "]\n" +
                "\tport=" + port + "\n" +
                "\tstorages=[" + storages + "]\n" +
                "\tindexes=[" + indexes + "]\n" +
                "\ttimeouts=[" + timeouts + "]\n";
    }
}
