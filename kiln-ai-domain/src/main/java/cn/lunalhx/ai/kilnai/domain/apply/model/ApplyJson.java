package cn.lunalhx.ai.kilnai.domain.apply.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class ApplyJson {

    private static final ObjectMapper STRICT = new ObjectMapper()
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    private ApplyJson() {
    }

    public static JsonNode readTree(String json) {
        try {
            return STRICT.readTree(json);
        } catch (JsonProcessingException exception) {
            throw new ApplyDraftException("model output is not valid JSON: " + exception.getOriginalMessage());
        }
    }

    public static String write(Object value) {
        try {
            return STRICT.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to serialize execution data", exception);
        }
    }
}
