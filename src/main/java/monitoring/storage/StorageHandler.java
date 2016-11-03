package monitoring.storage;

import monitoring.Handler;
import monitoring.ServerManager;
import monitoring.utils.ResponseUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpStatus;
import spark.Request;
import spark.Response;

import static monitoring.utils.ResponseUtils.*;

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
                if (start == null) {
                    return getError("'start' query parameter not specified", HttpStatus.BAD_REQUEST_400, response, logger);
                }
                if (count == null) {
                    return getError("'count' query parameter not specified", HttpStatus.BAD_REQUEST_400, response, logger);
                }

                String url = "timestamp/" + start + "/" + count;
                logger.debug("Parameters for /storageData: count=" + count + ", start=" + start);

                try {
                    return getOk(makeRequest(url), HttpStatus.OK_200, response, logger);
                } catch (RuntimeException e) {
                    return getError("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR_500, response, logger);
                }
            }

            case "/storageGetByKey/:key": {
                String key = request.params(":key");
                if (key == null) {
                    return getError("'key' parameter not specified", HttpStatus.BAD_REQUEST_400, response, logger);
                }

                try {
                    return getOk(makeRequest("key/" + key), HttpStatus.OK_200, response, logger);
                } catch (RuntimeException e) {
                    getError("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR_500, response, logger);
                }
            }

            default:
                response.status(404);
                return "Unknown method";
        }
    }
}
