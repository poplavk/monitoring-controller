package monitoring.web;

import monitoring.storage.StorageResponse;
import monitoring.web.request.TimeAndCountRequest;
import monitoring.web.request.TimeRequest;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
public class ClientAdapter {
    private static final Logger logger = LogManager.getLogger(ClientAdapter.class);

//    @RequestMapping(value = "/fromto", method = POST)
//    public ClientResponse requestWithBody(@RequestBody ClientRequest request) {
//        logger.info("Received object " + request);
//        return new ClientResponse(new long[]{1L}, new long[]{2L});
//    }

    @RequestMapping(value = "/getdata", method = GET, params = {"starttime", "endtime"})
    public ResponseEntity<ClientResponse> timeRequest(@RequestParam("starttime") String from,
                                                      @RequestParam("endtime") String to) {
        logger.info("Received get request, from=" + from + "," + "to=" + to);

        try {
            TimeRequest request = new TimeRequest(Long.valueOf(from), Long.valueOf(to));
            ClientMessageHandler handler = new ClientMessageHandler();
            List<StorageResponse> responses = handler.handle(request);
        } catch (InterruptedException | ExecutionException | RuntimeException e) {
            logger.error("unexpected error", e);
            throw new UnexpectedError();
        } catch (TimeoutException e) {
            throw new MonitoringTimeoutException();
        }

        return ResponseEntity.ok(new ClientResponse(new long[]{1L}, new long[]{2L}));
    }

    @RequestMapping(value = "/getdata", method = GET, params = {"starttime", "count"})
    public ResponseEntity<ClientResponse> timeAndCount(@RequestParam(value = "starttime", defaultValue = "-1") String from,
                                       @RequestParam(value = "count", defaultValue = "-1") String count) {
        logger.info("Received get request, from=" + from + "," + "count=" + count);

        try {
            TimeAndCountRequest request = new TimeAndCountRequest(Long.parseLong(from), Integer.parseInt(count));
            ClientMessageHandler handler = new ClientMessageHandler();
            List<StorageResponse> responses = handler.handle(request);
        } catch (InterruptedException | ExecutionException | RuntimeException e) {
            logger.error("unexpected error", e);
            throw new UnexpectedError();
        } catch (TimeoutException e) {
            throw new MonitoringTimeoutException();
        }

        return ResponseEntity.ok(new ClientResponse(new long[]{1L}, new long[]{2L}));
    }

    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR, reason = "Timeout error")
    public class MonitoringTimeoutException extends RuntimeException {}

    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR, reason = "Unexpected error")
    public class UnexpectedError extends RuntimeException {}
}
