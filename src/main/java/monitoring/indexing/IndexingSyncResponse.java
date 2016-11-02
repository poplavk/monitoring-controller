package monitoring.indexing;

import java.util.List;

public class IndexingSyncResponse {
    private String status;
    private String timestamp;
    private String count;
    private List<IndexingResponseChunk> keys;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getCount() {
        return count;
    }

    public void setCount(String count) {
        this.count = count;
    }

    public List<IndexingResponseChunk> getKeys() {
        return keys;
    }

    public void setKeys(List<IndexingResponseChunk> keys) {
        this.keys = keys;
    }
}
