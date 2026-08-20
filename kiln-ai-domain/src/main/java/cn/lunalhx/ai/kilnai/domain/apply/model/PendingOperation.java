package cn.lunalhx.ai.kilnai.domain.apply.model;

import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.pedagogy.FeedbackFacts;
import cn.lunalhx.ai.kilnai.domain.learning.pedagogy.TeachingAction;

import java.util.Objects;
import java.util.UUID;

/**
 * The durable, application-owned description of the operation an Unavailable
 * Interaction can resume (CONTEXT.md). It carries only the identity and
 * committed inputs needed to continue from durable state — never a
 * client-supplied replacement answer. A submission-evaluation resume carries
 * only the closed Attempt and the missing responsibility identity. The
 * initial unavailable boundary has retry count zero; each failed
 * {@code retry_requested} increments it; Phase 0 permits three failed
 * retries, then only Flow Control remains.
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
        UUID attemptId,
        String responsibility,
        String evaluationVersion,
        int failedRetryCount
) {

    public static final int MAX_FAILED_RETRIES = 3;

    public PendingOperation {
        Objects.requireNonNull(kind, "kind must not be null");
        if (failedRetryCount < 0) {
            throw new IllegalArgumentException("failedRetryCount must not be negative");
        }
        if (kind == Kind.RESUME_SUBMISSION_EVALUATION) {
            Objects.requireNonNull(attemptId, "attemptId must not be null for evaluation resume");
            Objects.requireNonNull(responsibility,
                    "responsibility must not be null for evaluation resume");
            Objects.requireNonNull(evaluationVersion,
                    "evaluationVersion must not be null for evaluation resume");
            if (responsibility.isBlank() || evaluationVersion.isBlank()) {
                throw new IllegalArgumentException(
                        "evaluation resume responsibility identity must not be blank");
            }
        } else if (attemptId != null || responsibility != null || evaluationVersion != null) {
            throw new IllegalArgumentException(
                    "only evaluation resume operations carry evaluation identity");
        }
    }

    public static PendingOperation resumeSubmissionEvaluation(
            UUID attemptId,
            String responsibility,
            String evaluationVersion
    ) {
        return new PendingOperation(
                Kind.RESUME_SUBMISSION_EVALUATION,
                null, null, null, null, null, null, null,
                attemptId, responsibility, evaluationVersion, 0);
    }

    public boolean retryAdvertised() {
        return failedRetryCount < MAX_FAILED_RETRIES;
    }

    public PendingOperation withFailedRetry() {
        return new PendingOperation(
                kind, action, decisionContext, facts, learnerMessage, intent, evidence, hint,
                attemptId, responsibility, evaluationVersion, failedRetryCount + 1);
    }

    public enum Kind {
        EXECUTE_MOVE,
        DELIVER_INDEPENDENT,
        DELIVER_INDEPENDENT_REPLACEMENT,
        RESUME_SUBMISSION_EVALUATION
    }
}
