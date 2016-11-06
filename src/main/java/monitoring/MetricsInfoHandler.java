package monitoring;

import monitoring.config.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpStatus;
import spark.Request;
import spark.Response;

import java.util.List;

import static monitoring.utils.ResponseUtils.getError;
import static monitoring.utils.ResponseUtils.getOk;


public class MetricsInfoHandler extends Handler {
    private static final Logger logger = LogManager.getLogger(MetricsInfoHandler.class);

    private MetricsInfoTable table;
    private Configuration config;

    public MetricsInfoHandler(MetricsInfoTable table, Configuration config) {
        this.table = table;
        this.config = config;
    }

    @Override
    public String handle(String method, Request request, Response response) throws Exception {
        switch (method) {
            case "/getMetricsTable": {
                try {
                    return getOk(table.getAllMetricsInfoAsJson(), HttpStatus.OK_200, response, logger);
                } catch (RuntimeException e) {
                    return getError("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR_500, response, logger);
                }
            }

            case "/startMonitoring": {
                String host = request.queryParams("host");
                String port = request.queryParams("port");
                String type = request.queryParams("type");
                if (host == null || port == null || type == null) {
                    return getError("Some of mandatory params not present: host/port/type", HttpStatus.BAD_REQUEST_400, response, logger);
                }
                if (!config.supportedMetricTypes.contains(type.toLowerCase())) {
                    return getError("Unsupported metric type: " + type + ", supported types are: " + String.join(",", config.supportedMetricTypes),
                            HttpStatus.BAD_REQUEST_400, response, logger);
                }

                table.addMetricInfo(host, port, type);
                return getOk("Successfully added", HttpStatus.OK_200, response, logger);
            }

            case "/stopMonitoring": {
                String host = request.queryParams("host");
                String port = request.queryParams("port");
                String type = request.queryParams("type");
                if (host == null || port == null || type == null) {
                    return getError("Some of mandatory params not present: host/port/type", HttpStatus.BAD_REQUEST_400, response, logger);
                }
                if (!config.supportedMetricTypes.contains(type.toLowerCase())) {
                    return getError("Unsupported metric type: " + type + ", supported types are: " + String.join(",", config.supportedMetricTypes),
                            HttpStatus.BAD_REQUEST_400, response, logger);
                }

                Long prev = table.removeMetricInfo(host, port, type);
                if (prev == null) {
                    return getError("No previous value for specified parameter", HttpStatus.INTERNAL_SERVER_ERROR_500, response, logger);
                } else return getOk("Successfully removed from table", HttpStatus.OK_200, response, logger);
            }

            default:
                return getError("Unknown method", HttpStatus.BAD_REQUEST_400, response, logger);
        }
    }
}
