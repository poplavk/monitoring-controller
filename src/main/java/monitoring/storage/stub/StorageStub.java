package monitoring.storage.stub;


import com.fasterxml.jackson.databind.ObjectMapper;
import monitoring.storage.StorageResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpStatus;

import static spark.Spark.get;
import static spark.Spark.port;

public class StorageStub {
    private static final Logger logger = LogManager.getLogger(StorageStub.class);

    public static void main(String[] args) {
        final int port = 8082;

        ObjectMapper mapper = new ObjectMapper();

        port(port);
        get("/key/:key", (req, res) -> {
            String key = req.params(":key");
            if (key == null) {
                logger.error("key param is null");
                res.status(HttpStatus.BAD_REQUEST_400);
                return "key param is null";
            }

            logger.info("received /key/" + key);

            return mapper.writeValueAsString(
                    new StorageResponse(key, String.valueOf(System.currentTimeMillis()), key + "_value")
            );
        });

        logger.info("Started to listen on localhost:" + port);
    }

}
