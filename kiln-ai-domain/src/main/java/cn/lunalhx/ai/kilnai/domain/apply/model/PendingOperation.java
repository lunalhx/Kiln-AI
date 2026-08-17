package cn.lunalhx.ai.kilnai.domain.apply.model;

import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.pedagogy.FeedbackFacts;
import cn.lunalhx.ai.kilnai.domain.learning.pedagogy.TeachingAction;

import java.util.Objects;

/**
 * The durable, application-owned description of the operation an Unavailable
 * Interaction can resume (CONTEXT.md). It carries only the identity and
 * committed inputs needed to continue from durable state — never a
 * client-supplied replacement answer. The initial unavailable boundary has
 * retry count zero; each failed {@code retry_requested} increments it;
 * Phase 0 permits three failed retries, then only Flow Control remains.
 */
public record PendingOperation(
        Kind kind,
        TeachingAction action,
        String decisionContext,
        FeedbackFacts facts,
        String learnerMessage,
        String intent,
        AcceptedLearningEvidence evidence,
        HintView hint,
        int failedRetryCount
) {

    public static final int MAX_FAILED_RETRIES = 3;

    public PendingOperation {
        Objects.requireNonNull(kind, "kind must not be null");
        if (failedRetryCount < 0) {
            throw new IllegalArgumentException("failedRetryCount must not be negative");
        }
    }

    public boolean retryAdvertised() {
        return failedRetryCount < MAX_FAILED_RETRIES;
    }

    public PendingOperation withFailedRetry() {
        return new PendingOperation(
                kind, action, decisionContext, facts, learnerMessage, intent, evidence, hint,
                failedRetryCount + 1);
    }

    public enum Kind {
        EXECUTE_MOVE,
        DELIVER_INDEPENDENT,
        DELIVER_INDEPENDENT_REPLACEMENT
    }
}
