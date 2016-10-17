package monitoring.indexing;

import monitoring.session.ClientSession;
import monitoring.session.ClientSessionManager;
import monitoring.storage.StorageAdapter;
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

@Deprecated
public class IndexingAdapter {
    private final Logger logger = LogManager.getLogger(IndexingAdapter.class);
    private final ConcurrentHashMap<URL, IndexingInputAdapter> inputAdapters;
    private final ConcurrentHashMap<URL, IndexingOutputAdapter> outputAdapters;
    private final List<IndexingOutputAdapter> roundRobinAdapters = new ArrayList<>();
    private AtomicInteger currentRRidx = new AtomicInteger(0);
    private ExecutorService executor;

    private IndexingAdapter() {
        inputAdapters = new ConcurrentHashMap<>();
        outputAdapters = new ConcurrentHashMap<>();
        executor = Executors.newCachedThreadPool();
    }

    private static IndexingAdapter instance;

    public static IndexingAdapter instance() {
        if (instance == null) {
            instance = new IndexingAdapter();
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

        IndexingInputAdapter inputAdapter = new IndexingInputAdapter(httpURLConnection);
        IndexingOutputAdapter outputAdapter = new IndexingOutputAdapter(httpURLConnection);

        inputAdapters.put(url, inputAdapter);
        outputAdapters.put(url, outputAdapter);
        roundRobinAdapters.add(outputAdapter);

        executor.execute(inputAdapter);
        executor.execute(outputAdapter);
    }

    public void stop(URL url) {
        IndexingInputAdapter inputAdapter = inputAdapters.get(url);
        if (inputAdapter != null) {
            try {
                inputAdapter.stop();
            } catch (IOException e) {
                logger.error("Could not stop input handler for URL " + url);
            }
        }

        IndexingOutputAdapter outputAdapter = outputAdapters.get(url);
        if (outputAdapter != null) {
            outputAdapter.stop();
            roundRobinAdapters.remove(outputAdapter);
        }
    }

    public void send(String messageOut) {
        int nextAdapter = robin();
        IndexingOutputAdapter outputAdapter = roundRobinAdapters.get(nextAdapter);
        outputAdapter.enqueue(messageOut);
        logger.debug("Enqueued message to " + outputAdapter.connection.getURL());
    }

    public void send(String messageOut, URL target) {
        IndexingOutputAdapter outputAdapter = outputAdapters.get(target);
        if (outputAdapter != null) {
            outputAdapter.enqueue(messageOut);
        } else {
            logger.error("No output indexing adapter found for URL " + target);
            // TODO: 13.10.2016 parse session id, complete future with failure and remove session
        }
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

    public URL nextIndexing() {
        synchronized (this) {
            IndexingOutputAdapter outputAdapter = roundRobinAdapters.get(robin());
            return outputAdapter.connection.getURL();
        }
    }

    /**
     * Processes only input messages from indexing service
     **/
    public class IndexingInputAdapter implements Runnable {
        private final Logger logger = LogManager.getLogger(IndexingInputAdapter.class);

        private BufferedReader reader;
        private AtomicBoolean isRunning = new AtomicBoolean(true);

        private final HttpURLConnection connection;

        public IndexingInputAdapter(HttpURLConnection connection) throws IOException {
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
                    logger.error("Error reading from input connection " + connection.getURL(), e);
                }
            }
        }

        private String receiveMessage() throws IOException {
            // TODO: 13.10.2016 format of input data to read
            return reader.readLine();
        }

        private void processMessage(String messageIn) {
            // TODO: 13.10.2016 parse client session from response and send them to fcking storage
            UUID sessionId = null;
            ClientSession session = ClientSessionManager.instance().getSession(sessionId);
            if (session != null) {
                StorageAdapter.instance().send(messageIn, session.chosenStorageUrl);
                logger.debug("Sent message " + messageIn + " to storage " + session.chosenStorageUrl);
            } else {
                logger.error("No client session for id " + sessionId + ", probably session expired");
            }
        }

        public void stop() throws IOException {
            isRunning.set(false);
            reader.close();
        }
    }

    /**
     * Processes only output messages that are needed to be sent to indexing service
     **/
    public class IndexingOutputAdapter implements Runnable {
        private final Logger logger = LogManager.getLogger(IndexingOutputAdapter.class);

        private AtomicBoolean isRunning = new AtomicBoolean(true);
        private ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<String>(1000);
        private final HttpURLConnection connection;
        private PrintWriter writer;

        public IndexingOutputAdapter(HttpURLConnection connection) throws IOException {
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
