package monitoring.offline;

import monitoring.Handler;
import monitoring.ServerManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpStatus;
import spark.Request;
import spark.Response;

import java.util.concurrent.ExecutionException;

import static monitoring.utils.ResponseUtils.getError;

public class OfflineHandler extends Handler {
    private static final Logger logger = LogManager.getLogger(OfflineHandler.class);

    public OfflineHandler(ServerManager manager) {
        this.manager = manager;
    }

    public String handle(String method, Request request, Response response)
        throws ExecutionException, InterruptedException {
        switch (method) {
            case "/offlineStatus": {
                try {
                    return makeRequest("status");
                } catch (RuntimeException e) {
                    return getError("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR_500, response, logger);
                }
            }

            case "/offlineStart": {
                try {
                    return makeRequest("start");
                } catch (RuntimeException e) {
                    return getError("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR_500, response, logger);
                }
            }

            case "/offlineStop": {
                try {
                    return makeRequest("stop");
                } catch (RuntimeException e) {
                    return getError("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR_500, response, logger);
                }
            }

            case "/offlineNewTask": {
                String task = request.queryParams("task");
                String metric = request.queryParams("metric");
                String from = request.queryParams("from");
                String to = request.queryParams("to");
                String calcStart = request.queryParams("calcStart");
                if (task == null || metric == null || from == null || to == null || calcStart == null) {
                    return getError("Not enough parameters", HttpStatus.BAD_REQUEST_400, response, logger);
                }

                try {
                    return makeRequest(
                        "task/new?task=" + task + "&" + "metric=" + metric + "&" +
                        "from=" + from + "&" + "to=" + to + "&" + "calcStart=" + calcStart);
                } catch (RuntimeException e) {
                    return getError("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR_500, response, logger);
                }
            }

            case "/offlineTask/:id/discard": {
                String id = request.params(":id");
                if (id == null) {
                    return getError(
                        "No id parameter in task status request", HttpStatus.BAD_REQUEST_400, response, logger
                    );
                }

                try {
                    return makeRequest("task/" + id + "/discard");
                } catch (RuntimeException e) {
                    return getError("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR_500, response, logger);
                }
            }

            case "/offlineTask/:id/status": {
                String id = request.params(":id");
                if (id == null) {
                    return getError(
                        "No id parameter in task status request", HttpStatus.BAD_REQUEST_400, response, logger
                    );
                }

                try {
                    return makeRequest("task/" + id + "/status");
                } catch (RuntimeException e) {
                    return getError("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR_500, response, logger);
                }
            }

            case "/offlineTask/:id/result": {
                String id = request.params(":id");
                if (id == null) {
                    return getError(
                        "No id parameter in task result request", HttpStatus.BAD_REQUEST_400, response, logger
                    );
                }

                try {
                    return makeRequest("task/" + id + "/result");
                } catch (RuntimeException e) {
                    return getError("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR_500, response, logger);
                }
            }

            default:
                return "Unknown method";
        }
    }
}
