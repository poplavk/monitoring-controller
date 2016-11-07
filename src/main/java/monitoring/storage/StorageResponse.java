package monitoring.storage;

public class StorageResponse {
    private String key;

    private String ts;

    private String value;

    public StorageResponse(String key, String ts, String value) {
        this.key = key;
        this.ts = ts;
        this.value = value;
    }

    // needed for fasterxml
    public StorageResponse() {}

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getTs() {
        return ts;
    }

    public void setTs(String ts) {
        this.ts = ts;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Key=" + key + ", timestamp=" + ts + ", value=" + value;
    }
}
