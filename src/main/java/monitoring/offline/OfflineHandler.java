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
import static monitoring.utils.ResponseUtils.getOk;

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
                    return getOk(makeRequest("status"), HttpStatus.OK_200, response, logger);
                } catch (RuntimeException e) {
                    return getError("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR_500, response, logger);
                }
            }

            case "/offlineStart": {
                try {
                    return getOk(makeRequest("start"), HttpStatus.OK_200, response, logger);
                } catch (RuntimeException e) {
                    return getError("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR_500, response, logger);
                }
            }

            case "/offlineStop": {
                try {
                    return getOk(makeRequest("stop"), HttpStatus.OK_200, response, logger);
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
                    return getOk(makeRequest(
                            "task/new?task=" + task + "&" + "metric=" + metric + "&" +
                            "from=" + from + "&" + "to=" + to + "&" + "calcStart=" + calcStart), HttpStatus.OK_200, response, logger);
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
                    return getOk(makeRequest("task/" + id + "/discard"), HttpStatus.OK_200, response, logger);
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
                    return getOk(makeRequest("task/" + id + "/status"), HttpStatus.OK_200, response, logger);
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
                    return getOk(makeRequest("task/" + id + "/result"), HttpStatus.OK_200, response, logger);
                } catch (RuntimeException e) {
                    return getError("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR_500, response, logger);
                }
            }

            default:
                return "Unknown method";
        }
    }
}
