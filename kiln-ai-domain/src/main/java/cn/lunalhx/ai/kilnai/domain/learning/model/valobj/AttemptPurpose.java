package cn.lunalhx.ai.kilnai.domain.learning.model.valobj;

import com.fasterxml.jackson.annotation.JsonValue;

public enum AttemptPurpose {
    DIAGNOSTIC("diagnostic"),
    PRACTICE("practice"),
    INDEPENDENT_TEST("independent_test"),
    REVIEW("review");

    private final String wireValue;

    AttemptPurpose(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String wireValue() {
        return wireValue;
    }
}
