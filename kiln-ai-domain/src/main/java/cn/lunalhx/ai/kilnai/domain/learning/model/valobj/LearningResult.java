package cn.lunalhx.ai.kilnai.domain.learning.model.valobj;

public enum LearningResult {
    PASS,
    PARTIAL,
    FAIL;

    public boolean isSuccessful() {
        return this == PASS;
    }
}
