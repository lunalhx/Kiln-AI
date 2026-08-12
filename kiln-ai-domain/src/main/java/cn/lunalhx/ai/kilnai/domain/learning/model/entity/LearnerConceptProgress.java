package cn.lunalhx.ai.kilnai.domain.learning.model.entity;

import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.ConceptState;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningEventType;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Aggregate for one learner's progress on one concept. */
public final class LearnerConceptProgress {

    private final UUID userId;
    private final UUID conceptId;
    private ConceptState state;
    private boolean hasIndependentSuccess;
    private boolean hasDelayedIndependentSuccess;
    private boolean hasTransferSuccess;
    private Instant lastIndependentSuccessAt;
    private Instant lastFailureAt;
    private Instant updatedAt;

    private LearnerConceptProgress(
            UUID userId, UUID conceptId, ConceptState state, boolean hasIndependentSuccess,
            boolean hasDelayedIndependentSuccess, boolean hasTransferSuccess,
            Instant lastIndependentSuccessAt, Instant lastFailureAt, Instant updatedAt
    ) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.conceptId = Objects.requireNonNull(conceptId, "conceptId must not be null");
        this.state = Objects.requireNonNull(state, "state must not be null");
        this.hasIndependentSuccess = hasIndependentSuccess;
        this.hasDelayedIndependentSuccess = hasDelayedIndependentSuccess;
        this.hasTransferSuccess = hasTransferSuccess;
        this.lastIndependentSuccessAt = lastIndependentSuccessAt;
        this.lastFailureAt = lastFailureAt;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static LearnerConceptProgress start(UUID userId, UUID conceptId, Instant startedAt) {
        return new LearnerConceptProgress(
                userId, conceptId, ConceptState.UNKNOWN, false, false, false,
                null, null, startedAt
        );
    }

    public static LearnerConceptProgress restore(
            UUID userId, UUID conceptId, ConceptState state, boolean hasIndependentSuccess,
            boolean hasDelayedIndependentSuccess, boolean hasTransferSuccess,
            Instant lastIndependentSuccessAt, Instant lastFailureAt, Instant updatedAt
    ) {
        return new LearnerConceptProgress(
                userId, conceptId, state, hasIndependentSuccess, hasDelayedIndependentSuccess,
                hasTransferSuccess, lastIndependentSuccessAt, lastFailureAt, updatedAt
        );
    }

    public ConceptState record(LearningEvidence evidence) {
        Objects.requireNonNull(evidence, "evidence must not be null");
        updatedAt = evidence.occurredAt();

        if (!evidence.result().isSuccessful()) {
            lastFailureAt = evidence.occurredAt();
            if (state == ConceptState.INDEPENDENT || state == ConceptState.DURABLE) {
                state = ConceptState.UNDERSTOOD;
                hasIndependentSuccess = false;
                hasDelayedIndependentSuccess = false;
                hasTransferSuccess = false;
                lastIndependentSuccessAt = null;
            }
            return state;
        }

        if (evidence.isIndependentSuccess()) {
            hasIndependentSuccess = true;
            lastIndependentSuccessAt = evidence.occurredAt();
            if (evidence.delayedReview()) {
                hasDelayedIndependentSuccess = true;
            }
            if (evidence.transfer()) {
                hasTransferSuccess = true;
            }
        }

        state = determineState(evidence);
        return state;
    }

    private ConceptState determineState(LearningEvidence evidence) {
        if (hasDelayedIndependentSuccess && hasTransferSuccess) {
            return ConceptState.DURABLE;
        }
        if (evidence.isIndependentSuccess() || hasIndependentSuccess) {
            return ConceptState.INDEPENDENT;
        }
        if (evidence.isAssistedSuccess()) {
            return ConceptState.ASSISTED;
        }
        if (evidence.eventType() == LearningEventType.TEACH_BACK) {
            return ConceptState.UNDERSTOOD;
        }
        return state;
    }

    public UUID userId() { return userId; }
    public UUID conceptId() { return conceptId; }
    public ConceptState state() { return state; }
    public boolean hasIndependentSuccess() { return hasIndependentSuccess; }
    public boolean hasDelayedIndependentSuccess() { return hasDelayedIndependentSuccess; }
    public boolean hasTransferSuccess() { return hasTransferSuccess; }
    public Instant lastIndependentSuccessAt() { return lastIndependentSuccessAt; }
    public Instant lastFailureAt() { return lastFailureAt; }
    public Instant updatedAt() { return updatedAt; }
}
