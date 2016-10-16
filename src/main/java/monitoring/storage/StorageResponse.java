package monitoring.storage;

public class StorageResponse {
    private DataUnit[] data;

    public DataUnit[] getData() {
        return data;
    }

    public void setData(DataUnit[] data) {
        this.data = data;
    }

    public class DataUnit {
        private String key;
        private String value;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
