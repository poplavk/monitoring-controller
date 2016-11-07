package monitoring.indexing;

import java.util.List;

public class IndexingSyncResponse {
    private String status;
    private String timestamp;
    private String count;
    private List<IndexingResponsePart> keys;

    public IndexingSyncResponse(String status, String timestamp, String count, List<IndexingResponsePart> keys) {
        this.status = status;
        this.timestamp = timestamp;
        this.count = count;
        this.keys = keys;
    }

    // needed for fasterxml
    public IndexingSyncResponse() {}

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

    public List<IndexingResponsePart> getKeys() {
        return keys;
    }

    public void setKeys(List<IndexingResponsePart> keys) {
        this.keys = keys;
    }
}
