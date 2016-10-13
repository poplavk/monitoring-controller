package monitoring.web;

import monitoring.ClientSession;
import monitoring.ClientSessionManager;
import monitoring.IndexingAdapter;
import monitoring.StorageAdapter;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class ClientAdapter {
    private static final Logger logger = LogManager.getLogger(ClientAdapter.class);

    @RequestMapping(value = "/fromto", method = POST)
    public ClientResponse requestWithBody(@RequestBody ClientRequest request) {
        logger.info("Received object " + request);
        return new ClientResponse(new long[] {1L}, new long[] {2L});
    }

    @RequestMapping(value = "/fromto", method = GET)
    public ClientResponse requestWithParams(@RequestParam(value = "from", defaultValue = "0") String from,
                                            @RequestParam(value = "to", defaultValue = "0") String to) {
        logger.info("Received object, from=" + from + "," + "to=" + to);

        URL indexing = IndexingAdapter.instance().nextIndexing();
        URL storage = StorageAdapter.instance().nextStorage();

        ClientSession session = new ClientSession(indexing, storage);
        ClientSessionManager.instance().addSession(session);

        IndexingAdapter.instance().send("FUCK YOU", indexing);

        logger.debug("Starting to wait for 5000L");
        try {
            boolean isOkay = session.promise.get(5000L, TimeUnit.MILLISECONDS);
            // TODO: 13.10.2016 respond to client!
            if (isOkay) {
                return new ClientResponse(new long[] {1L}, new long[] {1L});
            } else {
                return new ClientResponse(new long[] {6L}, new long[] {6L});
            }
        } catch (ExecutionException | TimeoutException | InterruptedException e) {
            logger.error("Fucking error", e);
            return new ClientResponse(new long[] {6L}, new long[] {6L});
        }
    }
}
