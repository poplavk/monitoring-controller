package monitoring.indexing;


public class IndexingResponseChunk {
    private String key;

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
