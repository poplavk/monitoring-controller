package monitoring.indexing.stub;

import com.fasterxml.jackson.databind.ObjectMapper;
import monitoring.indexing.IndexingResponsePart;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.OutputStream;
import java.nio.charset.Charset;

import static spark.Spark.*;

public class IndexingStub {
    private static final Logger logger = LogManager.getLogger(IndexingStub.class);

    // TODO: 30.10.2016 needs fixing with new API formats
    public static void main(String[] args) {
        final String fromField = "starttime";
        final String toField = "endtime";
        final String countField = "count";
        final int port = 8081;

        ObjectMapper objectMapper = new ObjectMapper();

        port(port);
        get("/getdata", (req, res) -> {
            String fromParam = req.queryParams(fromField);
            String toParam = req.queryParams(toField);
            String countParam = req.queryParams(countField);

            if (toParam != null) {
                logger.debug("Received request with non-null " + toField + " request field");
                IndexingResponsePart response = new IndexingResponsePart();
                response.setKey("testKey");

                res.status(200);
                String responseString = objectMapper.writeValueAsString(response);

                logger.info("Returning to response " + responseString);
                return responseString;
            } else if (countParam != null) {
                logger.debug("Received request with non-null " + countField + " request field");

                IndexingResponsePart response1 = new IndexingResponsePart();
                response1.setKey("testKey1");

                IndexingResponsePart response2 = new IndexingResponsePart();
                response2.setKey("testKey2");

                IndexingResponsePart response3 = new IndexingResponsePart();
                response3.setKey("testKey3");

                res.status(200);
                String responseString1 = objectMapper.writeValueAsString(response1);
                String responseString2 = objectMapper.writeValueAsString(response2);
                String responseString3 = objectMapper.writeValueAsString(response3);

                OutputStream out = res.raw().getOutputStream();
                out.write(responseString1.getBytes(Charset.forName("UTF-8")));
                out.write("@".getBytes(Charset.forName("UTF-8")));
                out.flush();

                out.write(responseString2.getBytes(Charset.forName("UTF-8")));
                out.write("@".getBytes(Charset.forName("UTF-8")));
                out.flush();

                out.write(responseString3.getBytes(Charset.forName("UTF-8")));
                out.flush();
                halt();

                logger.info("Returning count response " + responseString1);
                return responseString1;
            } else {
                res.status(404);
                return "Not found!";
            }
        });

        logger.info("Started to listen on localhost:" + port);
    }
}
