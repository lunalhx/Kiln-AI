package cn.lunalhx.ai.kilnai.domain.apply.model;

/**
 * Why a hint request could not produce a ladder. A Source Gap ends generation
 * immediately because approved material is insufficient; Node Execution
 * Failed is reached when the ladder still fails the Hint Gate after the one
 * allowed repair. Neither exposes any partial content, and the open Practice
 * Attempt remains untouched.
 */
public enum HintUnavailableReason {
    SOURCE_GAP,
    NODE_EXECUTION_FAILED
}
