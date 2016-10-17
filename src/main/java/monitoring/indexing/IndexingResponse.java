package monitoring.indexing;

import java.util.Arrays;

public class IndexingResponse {
    private String status;
    private String[] data;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String[] getData() {
        return data;
    }

    public void setData(String[] data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Status: " + status + "," + "data: " + Arrays.toString(data);
    }
}
