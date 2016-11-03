package monitoring.dataconsuming;

import monitoring.Handler;
import monitoring.ServerManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpStatus;
import spark.Request;
import spark.Response;

import java.util.concurrent.ExecutionException;

import static monitoring.utils.ResponseUtils.getError;
import static monitoring.utils.ResponseUtils.getOk;

public class DataConsumingHandler extends Handler {
    private static final Logger logger = LogManager.getLogger(DataConsumingHandler.class);

    public DataConsumingHandler(ServerManager manager) {
        this.manager = manager;
    }

    public String handle(String method, Request request, Response response)
        throws ExecutionException, InterruptedException {
        switch (method) {
            case "/dataStatus/:id": {
                String id = request.params(":id");
                if (id == null) {
                    return getError("Parameter id is not specified", HttpStatus.BAD_REQUEST_400, response, logger);
                }

                try {
                    return getOk(makeRequest("status/" + id), HttpStatus.OK_200, response, logger);
                } catch (RuntimeException e) {
                    return getError("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR_500, response, logger);
                }
            }

            case "/dataStart": {
                String port = request.queryParams("port");
                if (port == null) {
                    return getError("No port parameter specified", HttpStatus.BAD_REQUEST_400, response, logger);
                }

                try {
                    return getOk(makeRequest("start/?port=" + port), HttpStatus.OK_200, response, logger);
                } catch (RuntimeException e) {
                    return getError("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR_500, response, logger);
                }
            }

            case "/dataStop/:id": {
                String id = request.params(":id");
                if (id == null) {
                    return getError("No id at stop command", HttpStatus.BAD_REQUEST_400, response, logger);
                }

                try {
                    return getOk(makeRequest("stop/" + id), HttpStatus.OK_200, response, logger);
                } catch (RuntimeException e) {
                    return getError("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR_500, response, logger);
                }
            }

            case "/dataRestart/:id": {
                String id = request.params(":id");
                if (id == null) {
                    return getError("No id at stop command", HttpStatus.BAD_REQUEST_400, response, logger);
                }

                try {
                    return getOk(makeRequest("restart/" + id), HttpStatus.OK_200, response, logger);
                } catch (RuntimeException e) {
                    return getError("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR_500, response, logger);
                }
            }

            case "/dataAllstatus": {
                try {
                    return getOk(makeRequest("allstatus"), HttpStatus.OK_200, response, logger);
                } catch (RuntimeException e) {
                    return getError("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR_500, response, logger);
                }
            }

            default:
                return "Unknown method";
        }
    }
}
