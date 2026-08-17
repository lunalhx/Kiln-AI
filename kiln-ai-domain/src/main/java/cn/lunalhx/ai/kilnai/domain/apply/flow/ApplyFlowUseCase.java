package cn.lunalhx.ai.kilnai.domain.apply.flow;

import cn.lunalhx.ai.kilnai.domain.apply.ApplyHash;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.DiagnosticApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyCheckpoint;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyDeliveryResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyFlowInteraction;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyFlowResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.DiagnosticSubmissionResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.IndependentSubmissionResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.InteractionKind;
import cn.lunalhx.ai.kilnai.domain.apply.model.ReviewSubmissionResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.SourceArtifact;
import cn.lunalhx.ai.kilnai.domain.apply.model.SubmissionIgnoreReason;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.OperatorModelProfilePort;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore.ProcessedCommand;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

/**
 * The durable orchestrator of one learner's complete Apply flow. It owns
 * idempotency replay for start and submission commands, interaction-version
 * conflict detection, the Learner Interaction Boundary commits (interaction
 * plus checkpoint plus processed command, atomically), and recovery: every
 * boundary and artifact is persisted, so a fresh instance can resume an open
 * or closed Attempt and a replayed idempotency key returns the original
 * result without ever creating a second Attempt or Evidence.
 *
 * <p>Atomicity follows the cells the ticket requires: a Task Package and its
 * open Attempt are one atomic write, a learner interaction with its
 * checkpoint and processed command is one atomic write, and closing an
 * Attempt with its single formal submission is one atomic write. The last
 * write of every request is the boundary commit that carries the command, so
 * a replayed key always finds its original result once the request completed.
 */
public final class ApplyFlowUseCase {

    private final ArtifactStore artifactStore;
    private final LearningFlowStore flowStore;
    private final DiagnosticFlow diagnosticFlow;
    private final IndependentSubmissionFlow independentFlow;
    private final ReviewSubmissionFlow reviewFlow;
    private final ApplyExecutionContext diagnosticContext;
    private final OperatorModelProfilePort modelProfilePort;
    private final Clock clock;

    public ApplyFlowUseCase(
            ArtifactStore artifactStore,
            LearningFlowStore flowStore,
            DiagnosticFlow diagnosticFlow,
            IndependentSubmissionFlow independentFlow,
            ReviewSubmissionFlow reviewFlow,
            ApplyExecutionContext diagnosticContext,
            OperatorModelProfilePort modelProfilePort,
            Clock clock
    ) {
        this.artifactStore = Objects.requireNonNull(artifactStore, "artifactStore must not be null");
        this.flowStore = Objects.requireNonNull(flowStore, "flowStore must not be null");
        this.diagnosticFlow = Objects.requireNonNull(diagnosticFlow, "diagnosticFlow must not be null");
        this.independentFlow = Objects.requireNonNull(independentFlow, "independentFlow must not be null");
        this.reviewFlow = Objects.requireNonNull(reviewFlow, "reviewFlow must not be null");
        this.diagnosticContext = Objects.requireNonNull(diagnosticContext, "diagnosticContext must not be null");
        this.modelProfilePort = Objects.requireNonNull(modelProfilePort, "modelProfilePort must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public ApplyFlowResult start(UUID learnerId, UUID idempotencyKey) {
        requireUuidKey(idempotencyKey);
        Objects.requireNonNull(learnerId, "learnerId must not be null");
        String hash = ApplyHash.sha256HexDelimited("start", learnerId);
        return FlowCommandReplay.replayOrRun(flowStore, idempotencyKey, hash,
                interaction -> new ApplyFlowResult.Boundary(interaction),
                () -> {
                    UUID flowId = UUID.randomUUID();
                    ModelProfile profile = modelProfilePort.resolve();
                    flowStore.insertFlow(new LearningFlowStore.FlowRecord(
                            flowId, learnerId, DiagnosticApplyFixture.CONCEPT_ID,
                            FlowStatus.READY, LearningStage.DIAGNOSTIC, profile, clock.instant()));
                    saveSourcePack();
                    ApplyDeliveryResult delivery = diagnosticFlow.startDiagnostic(flowId, profile);
                    ApplyFlowInteraction interaction = switch (delivery) {
                        case ApplyDeliveryResult.Delivered delivered -> new ApplyFlowInteraction(
                                InteractionKind.TASK, flowId, 1, FlowStatus.AWAITING_LEARNER_INPUT,
                                LearningStage.DIAGNOSTIC,
                                delivered.attempt().attemptId(), delivered.attempt().purpose(),
                                delivered.learnerProjection(), null, null, null, null);
                        case ApplyDeliveryResult.Unavailable unavailable -> new ApplyFlowInteraction(
                                InteractionKind.UNAVAILABLE, flowId, 1, FlowStatus.TERMINAL,
                                LearningStage.DIAGNOSTIC,
                                null, null, null, unavailable.learnerMessage(), null, null, null);
                    };
                    commitBoundary(interaction, idempotencyKey, hash);
                    return new ApplyFlowResult.Boundary(interaction);
                });
    }

    public ApplyFlowResult submit(
            UUID flowId,
            int interactionVersion,
            UUID idempotencyKey,
            UUID attemptId,
            String rawDerivative,
            String confirmedCanonical,
            String rationale
    ) {
        requireUuidKey(idempotencyKey);
        Objects.requireNonNull(flowId, "flowId must not be null");
        Objects.requireNonNull(attemptId, "attemptId must not be null");
        Objects.requireNonNull(rawDerivative, "rawDerivative must not be null");
        String hash = ApplyHash.sha256HexDelimited("submit", flowId, interactionVersion, attemptId, rawDerivative,
                confirmedCanonical, rationale);
        return FlowCommandReplay.replayOrRun(flowStore, idempotencyKey, hash,
                interaction -> new ApplyFlowResult.Boundary(interaction),
                () -> {
                    LearningFlowStore.FlowRecord flow = flowStore.findFlow(flowId)
                            .orElseThrow(() -> new ApplicationException(ErrorCode.FLOW_NOT_FOUND, "flow not found"));
                    ApplyFlowInteraction latest = flowStore.latestInteraction(flowId)
                            .orElseThrow(() -> new ApplicationException(ErrorCode.FLOW_NOT_FOUND, "flow not found"));
                    if (latest.interactionVersion() != interactionVersion) {
                        throw new ApplicationException(ErrorCode.CONFLICT, "stale interactionVersion");
                    }
                    return routeSubmission(flow, latest, attemptId, rawDerivative, confirmedCanonical, rationale,
                            idempotencyKey, hash);
                });
    }

    public ApplyFlowInteraction query(UUID flowId) {
        Objects.requireNonNull(flowId, "flowId must not be null");
        return flowStore.latestInteraction(flowId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.FLOW_NOT_FOUND, "flow not found"));
    }

    private ApplyFlowResult routeSubmission(
            LearningFlowStore.FlowRecord flow,
            ApplyFlowInteraction latest,
            UUID attemptId,
            String rawDerivative,
            String confirmedCanonical,
            String rationale,
            UUID idempotencyKey,
            String hash
    ) {
        AttemptPurpose purpose = artifactStore.findAttempt(attemptId)
                .map(attempt -> attempt.purpose())
                .orElse(null);
        if (purpose == null) {
            return new ApplyFlowResult.SubmissionIgnored(SubmissionIgnoreReason.ATTEMPT_NOT_FOUND);
        }
        switch (purpose) {
            case DIAGNOSTIC -> {
                DiagnosticSubmissionResult result = diagnosticFlow.submitDiagnostic(
                        flow.flowId(), flow.modelProfile(), attemptId, rawDerivative, confirmedCanonical, rationale);
                return mapDiagnostic(latest, result, idempotencyKey, hash);
            }
            case INDEPENDENT_TEST -> {
                IndependentSubmissionResult result = independentFlow.submitIndependent(
                        flow, attemptId, rawDerivative, confirmedCanonical, rationale);
                return mapIndependent(latest, result, idempotencyKey, hash);
            }
            case REVIEW -> {
                ReviewSubmissionResult result = reviewFlow.submitReview(
                        flow, idempotencyKey, hash, attemptId, rawDerivative, confirmedCanonical, rationale);
                return mapReview(latest, result, idempotencyKey, hash);
            }
            default -> {
                return new ApplyFlowResult.SubmissionIgnored(SubmissionIgnoreReason.WRONG_ATTEMPT_PURPOSE);
            }
        }
    }

    private ApplyFlowResult mapDiagnostic(
            ApplyFlowInteraction latest,
            DiagnosticSubmissionResult result,
            UUID idempotencyKey,
            String hash
    ) {
        return switch (result) {
            case DiagnosticSubmissionResult.Passed passed -> boundary(
                    latest, LearningStage.INDEPENDENT_TEST, passed.independentAttempt().attemptId(),
                    passed.independentAttempt().purpose(), passed.independentLearnerProjection(), null,
                    idempotencyKey, hash);
            case DiagnosticSubmissionResult.Inconclusive inconclusive -> boundary(
                    latest, LearningStage.INDEPENDENT_TEST, inconclusive.independentAttempt().attemptId(),
                    inconclusive.independentAttempt().purpose(),
                    inconclusive.independentLearnerProjection(), null, idempotencyKey, hash);
            case DiagnosticSubmissionResult.Failed failed -> boundary(
                    latest, LearningStage.DIAGNOSTIC, null, null, null, DiagnosticFlow.SAFE_END_MESSAGE,
                    idempotencyKey, hash);
            case DiagnosticSubmissionResult.IndependentUnavailable unavailable -> boundary(
                    latest, LearningStage.DIAGNOSTIC, null, null, null, unavailable.learnerMessage(),
                    idempotencyKey, hash);
            case DiagnosticSubmissionResult.NotSubmittable notSubmittable ->
                    new ApplyFlowResult.SubmissionRejected(notSubmittable.reason());
            case DiagnosticSubmissionResult.Ignored ignored ->
                    new ApplyFlowResult.SubmissionIgnored(ignored.reason());
        };
    }

    private ApplyFlowResult mapIndependent(
            ApplyFlowInteraction latest,
            IndependentSubmissionResult result,
            UUID idempotencyKey,
            String hash
    ) {
        return switch (result) {
            case IndependentSubmissionResult.EvidenceAccepted accepted -> boundary(
                    latest, LearningStage.INDEPENDENT_TEST, null, null, null,
                    accepted.learnerMessage(), idempotencyKey, hash);
            case IndependentSubmissionResult.NoEvidence noEvidence -> boundary(
                    latest, LearningStage.INDEPENDENT_TEST, null, null, null,
                    noEvidence.learnerMessage(), idempotencyKey, hash);
            // The legacy Apply seam has no remediation loop: a conclusive
            // no-hint failure, a Blocked, or an Inconclusive outcome ends at
            // the safe terminal boundary without creating Evidence, exactly
            // like the pre-guard behavior. The graph-backed command surface
            // owns fail Evidence, milestone drops, and fresh replacements.
            case IndependentSubmissionResult.FailureEvidenceAccepted failed -> boundary(
                    latest, LearningStage.INDEPENDENT_TEST, null, null, null,
                    IndependentSubmissionFlow.SAFE_END_MESSAGE, idempotencyKey, hash);
            case IndependentSubmissionResult.ReplacementRequired replacement -> boundary(
                    latest, LearningStage.INDEPENDENT_TEST, null, null, null,
                    IndependentSubmissionFlow.SAFE_END_MESSAGE, idempotencyKey, hash);
            case IndependentSubmissionResult.NotSubmittable notSubmittable ->
                    new ApplyFlowResult.SubmissionRejected(notSubmittable.reason());
            case IndependentSubmissionResult.Ignored ignored ->
                    new ApplyFlowResult.SubmissionIgnored(ignored.reason());
        };
    }

    private ApplyFlowResult mapReview(
            ApplyFlowInteraction latest,
            ReviewSubmissionResult result,
            UUID idempotencyKey,
            String hash
    ) {
        return switch (result) {
            case ReviewSubmissionResult.EvidenceAccepted accepted -> boundary(
                    latest, LearningStage.DELAYED_REVIEW, null, null, null,
                    accepted.learnerMessage(), idempotencyKey, hash);
            case ReviewSubmissionResult.FailureEvidenceAccepted failed -> boundary(
                    latest, LearningStage.DELAYED_REVIEW, null, null, null,
                    failed.learnerMessage(), idempotencyKey, hash);
            case ReviewSubmissionResult.NoEvidence noEvidence -> boundary(
                    latest, LearningStage.DELAYED_REVIEW, null, null, null,
                    noEvidence.learnerMessage(), idempotencyKey, hash);
            case ReviewSubmissionResult.ReplacementBound bound ->
                    new ApplyFlowResult.Boundary(bound.interaction());
            case ReviewSubmissionResult.ReplacementUnavailable unavailable ->
                    new ApplyFlowResult.Boundary(unavailable.interaction());
            case ReviewSubmissionResult.NotSubmittable notSubmittable ->
                    new ApplyFlowResult.SubmissionRejected(notSubmittable.reason());
            case ReviewSubmissionResult.Ignored ignored ->
                    new ApplyFlowResult.SubmissionIgnored(ignored.reason());
        };
    }

    private ApplyFlowResult.Boundary boundary(
            ApplyFlowInteraction latest,
            LearningStage stage,
            UUID attemptId,
            AttemptPurpose purpose,
            cn.lunalhx.ai.kilnai.domain.apply.model.LearnerProjection learnerProjection,
            String learnerMessage,
            UUID idempotencyKey,
            String hash
    ) {
        FlowStatus status = learnerProjection == null ? FlowStatus.TERMINAL : FlowStatus.AWAITING_LEARNER_INPUT;
        ApplyFlowInteraction interaction = new ApplyFlowInteraction(
                learnerProjection == null ? InteractionKind.TRANSITION : InteractionKind.TASK,
                latest.flowId(), latest.interactionVersion() + 1, status, stage,
                attemptId, purpose, learnerProjection, learnerMessage, null, null, null);
        commitBoundary(interaction, idempotencyKey, hash);
        return new ApplyFlowResult.Boundary(interaction);
    }

    private void commitBoundary(ApplyFlowInteraction interaction, UUID idempotencyKey, String hash) {
        flowStore.commitBoundary(
                interaction,
                new ApplyCheckpoint(UUID.randomUUID(), interaction.flowId(),
                        interaction.interactionVersion(), clock.instant()),
                new ProcessedCommand(idempotencyKey, hash, interaction.flowId(), interaction, clock.instant()));
    }

    private void saveSourcePack() {
        ApplyExecutionContext.ConceptSourcePack pack = diagnosticContext.conceptSourcePack();
        artifactStore.saveSource(new SourceArtifact(pack.id(), pack.version(), pack.passages()));
    }

    private void requireUuidKey(UUID key) {
        if (key == null) {
            throw new ApplicationException(ErrorCode.INVALID_ARGUMENT, "Idempotency-Key is required");
        }
    }
}
