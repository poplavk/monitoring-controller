package monitoring;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


public class StorageAdapter {
    private final Logger logger = LogManager.getLogger(StorageAdapter.class);
    private final ConcurrentHashMap<URL, StorageInputAdapter> inputAdapters;
    private final ConcurrentHashMap<URL, StorageOutputAdapter> outputAdapters;
    private final List<StorageOutputAdapter> roundRobinAdapters = new ArrayList<>();
    private AtomicInteger currentRRidx = new AtomicInteger(0);
    private ExecutorService executor;

    private StorageAdapter() {
        inputAdapters = new ConcurrentHashMap<>();
        outputAdapters = new ConcurrentHashMap<>();
        executor = Executors.newCachedThreadPool();
    }
    private static StorageAdapter instance;

    public static StorageAdapter instance() {
        if (instance == null) {
            instance = new StorageAdapter();
        }
        return instance;
    }

    public void connect(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        if (!(connection instanceof HttpURLConnection)) {
            logger.error("Connection for clientUrl " + url + " is not instance of httpurlconnection");
            return;
        }

        HttpURLConnection httpURLConnection = (HttpURLConnection) connection;

        StorageInputAdapter inputAdapter = new StorageInputAdapter(httpURLConnection);
        StorageOutputAdapter outputAdapter = new StorageOutputAdapter(httpURLConnection);

        inputAdapters.put(url, inputAdapter);
        outputAdapters.put(url, outputAdapter);
        roundRobinAdapters.add(outputAdapter);

        executor.execute(inputAdapter);
        executor.execute(outputAdapter);
    }

    public void stop(URL url) {
        StorageInputAdapter inputAdapter = inputAdapters.get(url);
        if (inputAdapter != null) {
            try {
                inputAdapter.stop();
            } catch (IOException e) {
                logger.error("Could not stop input handler for URL " + url);
            }
        }

        StorageOutputAdapter outputAdapter = outputAdapters.get(url);
        if (outputAdapter != null) {
            outputAdapter.stop();
            roundRobinAdapters.remove(outputAdapter);
        }
    }

    public void send(String messageOut, URL target) {
        StorageOutputAdapter outputAdapter = outputAdapters.get(target);
        if (outputAdapter != null) {
            outputAdapter.enqueue(messageOut);
        } else logger.error("No storage output adapter for URL " + target);
    }

    /** Send by round robin **/
    public void send(String messageOut) {
        int nextAdapter = robin();
        StorageOutputAdapter outputAdapter = roundRobinAdapters.get(nextAdapter);
        outputAdapter.enqueue(messageOut);
        logger.debug("Enqueued message to storage service host " + outputAdapter.connection.getURL());
    }


    private int robin() {
        synchronized (this) {
            int nextIdx = currentRRidx.incrementAndGet();
            if (nextIdx >= roundRobinAdapters.size()) {
                nextIdx = 0;
                currentRRidx.set(0);
            }

            return nextIdx;
        }
    }

    public URL nextStorage() {
        synchronized (this) {
            StorageOutputAdapter outputAdapter = roundRobinAdapters.get(robin());
            return outputAdapter.connection.getURL();
        }
    }

    /** Processes only input messages from storage **/
    private class StorageInputAdapter implements Runnable {
        private final Logger logger = LogManager.getLogger(StorageInputAdapter.class);

        private BufferedReader reader;
        private AtomicBoolean isRunning = new AtomicBoolean(true);

        private final HttpURLConnection connection;

        public StorageInputAdapter(HttpURLConnection connection) throws IOException {
            this.connection = connection;
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        }

        @Override
        public void run() {
            while (isRunning.get()) {
                try {
                    String message = receiveMessage();
                    processMessage(message);
                } catch (IOException e) {
                    logger.error("Error reading from input connection from storage service host " + connection.getURL(), e);
                }
            }
        }

        private String receiveMessage() throws IOException {
            // TODO: 13.10.2016 format of input message
            return reader.readLine();
        }

        private void processMessage(String messageIn) {
            // TODO: 12.10.2016 parsing from JSON and processing storage message
            UUID sessionId = null; // suppose we have one
            ClientSession session = ClientSessionManager.instance().getSession(sessionId);
            if (session != null) {
                session.buffer.add(messageIn);
                if (isFinalMessage(sessionId, messageIn)) {
                    session.promise.complete(true);
                }
            }

        }

        public void stop() throws IOException {
            isRunning.set(false);
            reader.close();
        }

        /** Is it final message in stream? **/
        private boolean isFinalMessage(UUID sessionId, String message) {
            // TODO: 12.10.2016 how to determine if it is the end of stream?
            return false;
        }
    }

    /** Processes only output messages that are needed to be sent to storage service **/
    private class StorageOutputAdapter implements Runnable {
        private final Logger logger = LogManager.getLogger(StorageOutputAdapter.class);

        private AtomicBoolean isRunning = new AtomicBoolean(true);
        private ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<String>(1000);
        private final HttpURLConnection connection;
        private PrintWriter writer;

        public StorageOutputAdapter(HttpURLConnection connection) throws IOException {
            this.connection = connection;
            writer = new PrintWriter(connection.getOutputStream());
        }

        @Override
        public void run() {
            while (isRunning.get()) {
                String messageOut = queue.poll();
                if (messageOut != null) {
                    writer.write(messageOut);
                    writer.flush();
                }
            }
        }

        public void enqueue(String messageOut) {
            boolean success = queue.offer(messageOut);
            if (!success) {
                logger.error("Could not append message to queue for host " + connection.getURL());
            }
        }

        public void stop() {
            isRunning.set(false);
            writer.close();
            queue.clear();
        }
    }
}
