package cn.lunalhx.ai.kilnai.domain.apply.model;

/**
 * Why an Explain teaching artifact could not be prepared. Explain is a
 * non-task Profile: a Source Gap ends generation immediately, while any
 * repeated invalid envelope is Node Execution Failed. No separate Task
 * Verifier runs for Explain.
 */
public enum ExplainUnavailableReason {
    SOURCE_GAP,
    NODE_EXECUTION_FAILED,
    PROVIDER_UNAVAILABLE
}
