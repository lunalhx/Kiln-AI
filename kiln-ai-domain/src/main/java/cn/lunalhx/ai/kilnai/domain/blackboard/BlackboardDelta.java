package cn.lunalhx.ai.kilnai.domain.blackboard;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record BlackboardDelta(String channel, Map<String, Object> fields) {
    public BlackboardDelta {
        Objects.requireNonNull(channel, "channel must not be null");
        Objects.requireNonNull(fields, "fields must not be null");
        fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }
}
