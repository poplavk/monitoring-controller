package monitoring.indexing.stub;

import com.fasterxml.jackson.databind.ObjectMapper;
import monitoring.indexing.IndexingResponsePart;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpStatus;

import java.io.PrintWriter;

import static spark.Spark.*;

public class IndexingStub {
    private static final Logger logger = LogManager.getLogger(IndexingStub.class);

    public static void main(String[] args) {
        final int port = 8081;

        ObjectMapper mapper = new ObjectMapper();

        port(port);
        get("/getIndexData/:id/:timestamp", (req, res) -> {
            String id = req.params(":id");
            String timestamp = req.params(":timestamp");

            if (id == null || timestamp == null) {
                res.status(HttpStatus.BAD_REQUEST_400);
                return "Wrong request parameters";
            }

            boolean isStream = false;
            String streamHeader = req.headers("stream");
            if (streamHeader == null) {
                logger.warn("stream header is null");
            } else if (streamHeader.equalsIgnoreCase("true")) {
                logger.debug("Stream header = true");
                isStream = true;
            } else {
                logger.error("Stream header not equals true: " + streamHeader);
            }

            if (isStream) {
                IndexingResponsePart part1 = new IndexingResponsePart("key1");
                IndexingResponsePart part2 = new IndexingResponsePart("key2");
                IndexingResponsePart part3 = new IndexingResponsePart("key3");

                PrintWriter writer = res.raw().getWriter();
                writer.write(mapper.writeValueAsString(part1));
                writer.write("@");
                writer.flush();

                writer.write(mapper.writeValueAsString(part2));
                writer.write("@");
                writer.flush();

                writer.write(mapper.writeValueAsString(part3));
                writer.write("@");
                writer.flush();

                halt();
                return null;
            } else {
                res.status(HttpStatus.OK_200);
                return "{\"status\": \"ok\", \"timestamp\": \"12345\"," +
                        "\"count\": \"3\", \"keys\": [{\"key\":\"key1\"}, {\"key\":\"key2\"}, {\"key\":\"key3\"}]}";
            }
        });

        logger.info("Started to listen on localhost:" + port);
    }
}
