package monitoring.storage;

import monitoring.Handler;
import monitoring.ServerManager;
import monitoring.utils.ResponseUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpStatus;
import spark.Request;
import spark.Response;

public class StorageHandler extends Handler {
    private static final Logger logger = LogManager.getLogger(StorageHandler.class);
    private ServerManager storageManager;

    public StorageHandler(ServerManager manager) {
        this.manager = manager;
    }

    @Override
    public String handle(String method, Request request, Response response) throws Exception {
        switch (method) {
            case "/storageData": {
                String start = request.queryParams("start");
                String count = request.queryParams("count");
                boolean allRecords = false;
                String url = "timestamp/" + start;
                if (count != null) {
                    url += "/" + count;
                } else {
                    logger.warn("count parameter not specified for /storageData, will try to fetch all records starting from " + start);
                }

                try {
                    return makeRequest(url);
                } catch (RuntimeException e) {
                    return ResponseUtils.getError("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR_500, response, logger);
                }
            }

            default:
                response.status(404);
                return "Unknown method";
        }
    }
}
