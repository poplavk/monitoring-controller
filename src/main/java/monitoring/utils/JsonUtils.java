package monitoring.utils;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import monitoring.indexing.IndexingResponsePart;

import java.io.IOException;

public class JsonUtils {
    private static ObjectMapper mapper = new ObjectMapper();

    private JsonUtils() { }

    /**
     * Serialize anything
     * @throws RuntimeException if serialization fails
     */
    public static <T> String serialize(T object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param string
     * @return
     * @throws RuntimeException if deserialization failed
     */
    public static IndexingResponsePart indexingResponse(String string) {
        try {
            return mapper.readValue(string, IndexingResponsePart.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
