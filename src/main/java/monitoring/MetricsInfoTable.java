package monitoring;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MetricsInfoTable {
    private static final Logger logger = LogManager.getLogger(MetricsInfoTable.class);

    private Random random = new Random(System.currentTimeMillis());
    private ConcurrentHashMap<String, Long> table = new ConcurrentHashMap<>();

    public void addMetricInfo(String host, String port, String metricType) {
        long metricId = Math.abs(random.nextLong());
        table.put(host + "|" + port + "|" + metricType, metricId);
        logger.debug("Added metrics info to table: host=" + host + ", port=" + port + ", type=" + metricType);
    }

    public Long removeMetricInfo(String host, String port, String metricType) {
        return table.remove(host + "|" + port + "|" + metricType);
    }

    public Optional<Long> getMetricInfoId(String host, String port, String metricType) {
        return Optional.ofNullable(table.get(host + "|" + port + "|" + metricType));
    }

    public Map<String, Long> getAllMetricsInfo() {
        return table;
    }

    /** Return info about all registered metrics as JSON string **/
    public String getAllMetricsInfoAsJson() {
        List<String> info = table.entrySet().stream().map(entry -> {
            String[] splitted = entry.getKey().split("\\|");
            return "{" +
                    "\"metricId\":\"" + entry.getValue() + "\"," +
                    "\"host\":\"" + splitted[0] + "\"," +
                    "\"port\":\"" + splitted[1] + "\"," +
                    "\"metricType\":\"" + splitted[2] + "\"" +
                    "}";
        }).collect(Collectors.toList());

        return "{ \"metrics\" : [" + String.join(", ", info) + "] }";
    }
}
