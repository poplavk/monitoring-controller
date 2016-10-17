package monitoring.utils;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import monitoring.indexing.IndexingResponse;
import monitoring.storage.StorageResponse;

import java.io.IOException;
import java.util.List;

public class JsonUtils {
    private static ObjectMapper mapper = new ObjectMapper();

    /**
     * @param response
     * @return
     * @throws RuntimeException if serialization failed
     */
    public static String serialize(StorageResponse response) {
        try {
            return mapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String serialize(List<StorageResponse> responses) {
        try {
            return mapper.writeValueAsString(responses);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param string
     * @return
     * @throws RuntimeException if deserialization failed
     */
    public static IndexingResponse indexingResponse(String string) {
        try {

            return mapper.readValue(string, IndexingResponse.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
