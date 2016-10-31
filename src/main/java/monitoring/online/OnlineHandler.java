package monitoring.online;

import monitoring.Handler;
import monitoring.ServerManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpStatus;
import spark.Request;
import spark.Response;

import java.util.concurrent.ExecutionException;

import static monitoring.utils.ResponseUtils.getError;

public class OnlineHandler extends Handler {
    private static final Logger logger = LogManager.getLogger(OnlineHandler.class);

    public OnlineHandler(ServerManager manager) {
        this.manager = manager;
    }

    public String handle(String method, Request request, Response response)
        throws ExecutionException, InterruptedException {
        switch (method) {
            case "/onlineStart": {
                try {
                    return makeRequest("start/");
                } catch (RuntimeException e) {
                    return getError("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR_500, response, logger);
                }
            }

            case "/onlineStop/:id": {
                String id = request.params(":id");
                if (id == null) {
                    return getError("Parameter id is not specified", HttpStatus.BAD_REQUEST_400, response, logger);
                }

                try {
                    return makeRequest("stop/" + id);
                } catch (RuntimeException e) {
                    return getError("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR_500, response, logger);
                }
            }

            case "/onlineStatus": {
                try {
                    return makeRequest("status/");
                } catch (RuntimeException e) {
                    return getError("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR_500, response, logger);
                }
            }

            case "/onlineStatus/:id": {
                String id = request.params(":id");
                if (id == null) {
                    return getError("Parameter id is not specified", HttpStatus.BAD_REQUEST_400, response, logger);
                }

                try {
                    return makeRequest("status/" + id);
                } catch (RuntimeException e) {
                    return getError("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR_500, response, logger);
                }
            }

            default:
                return "Unknown method";
        }
    }
}
