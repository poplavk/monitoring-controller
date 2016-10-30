package monitoring.storage.stub;


import com.fasterxml.jackson.databind.ObjectMapper;
import monitoring.storage.StorageResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static spark.Spark.get;
import static spark.Spark.port;

public class StorageStub {
    private static final Logger logger = LogManager.getLogger(StorageStub.class);

    // TODO: 30.10.2016 need fixing with new API formats
    public static void main(String[] args) {
        final String fromField = "starttime";
        final String toField = "endtime";
        final String countField = "count";
        final int port = 8082;

        ObjectMapper objectMapper = new ObjectMapper();

        port(port);
        get("/getdata", (req, res) -> {
            String fromParam = req.queryParams(fromField);
            String toParam = req.queryParams(toField);
            String countParam = req.queryParams(countField);

            if (toParam != null) {
                logger.debug("Received request with non-null " + toField + " request field");
                StorageResponse response = new StorageResponse();
                response.setKey("testKey");
                response.setTs("testTs");
                response.setValue("testValue");

                res.status(200);
                String responseString = objectMapper.writeValueAsString(response);

                logger.info("Returning to response " + responseString);
                return responseString;
            } else if (countParam != null) {
                logger.debug("Received request with non-null " + countField + " request field");
                StorageResponse response = new StorageResponse();
                response.setKey("testKey");
                response.setTs("testTs");
                response.setValue("testValue");

                res.status(200);
                String responseString = objectMapper.writeValueAsString(response);

                logger.info("Returning count response " + responseString);
                return responseString;
            } else {
                res.status(404);
                return "Not found!";
            }
        });

        get("/test", (req, res) -> {
            logger.info("Received request for /test with body: " + req.body());
            StorageResponse response = new StorageResponse();
            response.setKey("testKey");
            response.setTs("testTs");
            response.setValue("testValue");

            res.status(200);
            String responseString = objectMapper.writeValueAsString(response);

            logger.info("Returning count response " + responseString);

            return responseString;
        });

        logger.info("Started to listen on localhost:" + port);
    }

}
