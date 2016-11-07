package monitoring.indexing;


public class IndexingResponsePart {
    private String key;

    public IndexingResponsePart(String key) {
        this.key = key;
    }

    // needed for fasterxml
    public IndexingResponsePart() {}

    @Override
    public String toString() {
        return "Key: " + key;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
