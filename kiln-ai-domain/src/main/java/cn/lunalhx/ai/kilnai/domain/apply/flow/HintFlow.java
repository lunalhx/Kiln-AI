package cn.lunalhx.ai.kilnai.domain.apply.flow;

import cn.lunalhx.ai.kilnai.domain.apply.gate.HintGateFacts;
import cn.lunalhx.ai.kilnai.domain.apply.gate.HintLadderGatePolicy;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyDraftException;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintExposureOutcome;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintGenerationDraft;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintLadder;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintUnavailableReason;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintView;
import cn.lunalhx.ai.kilnai.domain.apply.model.LearnerProjection;
import cn.lunalhx.ai.kilnai.domain.apply.model.PrivateAssessorProjection;
import cn.lunalhx.ai.kilnai.domain.apply.model.SubmissionIgnoreReason;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.HintGenerationPort;
import cn.lunalhx.ai.kilnai.domain.apply.profile.HintPromptCompiler;
import cn.lunalhx.ai.kilnai.domain.gate.GateContext;
import cn.lunalhx.ai.kilnai.domain.gate.GateOutcome;
import cn.lunalhx.ai.kilnai.domain.gate.GateResult;
import cn.lunalhx.ai.kilnai.domain.gate.TypedArtifactGatePipeline;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The reference Hint flow for an open Apply Practice Attempt. The first
 * request makes one model call to generate the full private H1-H5 ladder,
 * runs it through the Hint Ladder Gate with one allowed repair, and then the
 * store atomically persists the stable ladder, appends only the requested
 * level to the attempt's Assistance Trace, records the request, and closes
 * the attempt as Solution Revealed for H5. Later requests reveal persisted
 * levels deterministically without another model call. A Source Gap or a
 * Node Execution Failed exposes no partial content and leaves the Practice
 * Attempt open; a request for the answer may jump directly to H5. A command
 * that crashed between the exposure and its boundary commit resumes the same
 * exposed level from the saved request record. The flow writes no Learning
 * State; the graph owns the boundary.
 */
public final class HintFlow {

    public static final String HINT_UNAVAILABLE_MESSAGE = "暂时无法生成提示，请稍后重试或继续作答。";

    private static final int MAX_GENERATION_CYCLES = 2;

    private final HintGenerationPort generationPort;
    private final ArtifactStore artifactStore;
    private final ApplyExecutionContext.ConceptSourcePack sourcePack;
    private final HintPromptCompiler compiler;
    private final TypedArtifactGatePipeline gatePipeline;

    public HintFlow(
            HintGenerationPort generationPort,
            ArtifactStore artifactStore,
            ApplyExecutionContext.ConceptSourcePack sourcePack
    ) {
        this.generationPort = Objects.requireNonNull(generationPort, "generationPort must not be null");
        this.artifactStore = Objects.requireNonNull(artifactStore, "artifactStore must not be null");
        this.sourcePack = Objects.requireNonNull(sourcePack, "sourcePack must not be null");
        this.compiler = new HintPromptCompiler();
        this.gatePipeline = new TypedArtifactGatePipeline();
    }

    public HintResult requestHint(TaskAttempt attempt, boolean answerRequested, UUID commandKey) {
        Objects.requireNonNull(attempt, "attempt must not be null");
        Objects.requireNonNull(commandKey, "commandKey must not be null");
        // The saved request record is the resume point of a command that
        // crashed between its exposure and its boundary commit: it must be
        // checked before the open guards, because an H5 reveal already closed
        // the attempt as Solution Revealed and the retry still owes the
        // learner the reveal boundary.
        Optional<HintResult> resumed = resumeFromRequest(attempt, commandKey);
        if (resumed.isPresent()) {
            return resumed.get();
        }
        if (attempt.purpose() != AttemptPurpose.PRACTICE) {
            return new HintResult.Ignored(SubmissionIgnoreReason.WRONG_ATTEMPT_PURPOSE);
        }
        if (!attempt.isOpen()) {
            return new HintResult.Ignored(SubmissionIgnoreReason.ALREADY_SUBMITTED);
        }
        int requestedLevel = answerRequested ? 5 : attempt.highestHintLevel() + 1;
        HintLadder ladder = artifactStore.findLadder(attempt.attemptId()).orElse(null);
        if (ladder == null) {
            GeneratedLadder generated = generateLadder(attempt);
            if (generated instanceof GeneratedLadder.Unavailable unavailable) {
                return new HintResult.Unavailable(unavailable.reason(), HINT_UNAVAILABLE_MESSAGE);
            }
            ladder = ((GeneratedLadder.Ready) generated).ladder();
        }
        return expose(attempt, ladder, requestedLevel, commandKey);
    }

    /**
     * A request recorded by a prior run of this same command means the
     * exposure committed before the boundary did: rebuild the original
     * exposed view from the saved request and the stable ladder instead of
     * revealing the next level or regenerating anything.
     */
    private Optional<HintResult> resumeFromRequest(TaskAttempt attempt, UUID commandKey) {
        Optional<HintLadder> ladder = artifactStore.findLadder(attempt.attemptId());
        if (ladder.isEmpty()) {
            return Optional.empty();
        }
        return artifactStore.findHintRequest(attempt.attemptId(), commandKey)
                .map(request -> new HintResult.Revealed(
                        attempt, ladder.get().view(request.exposedLevel())));
    }

    private HintResult expose(TaskAttempt attempt, HintLadder ladder, int requestedLevel, UUID commandKey) {
        HintExposureOutcome outcome = artifactStore.exposeHint(
                attempt.attemptId(), ladder, requestedLevel, commandKey);
        return switch (outcome) {
            case HintExposureOutcome.Exposed exposed ->
                    new HintResult.Revealed(exposed.attempt(), ladder.view(exposed.request().exposedLevel()));
            case HintExposureOutcome.AlreadyExposed already ->
                    new HintResult.Revealed(already.attempt(), ladder.view(already.request().exposedLevel()));
            case HintExposureOutcome.NotOpen ignored ->
                    new HintResult.Ignored(SubmissionIgnoreReason.ALREADY_SUBMITTED);
            case HintExposureOutcome.NotFound ignored ->
                    new HintResult.Ignored(SubmissionIgnoreReason.ATTEMPT_NOT_FOUND);
        };
    }

    /**
     * The bounded generation cycle: one initial call and at most one same-plan
     * repair through the strict parser and the Hint Ladder Gate. A Source Gap
     * ends generation immediately; a repeated invalid result becomes Node
     * Execution Failed. Nothing is persisted for either failure.
     */
    private GeneratedLadder generateLadder(TaskAttempt attempt) {
        TaskPackage taskPackage = artifactStore.findPackage(attempt.taskPackageId()).orElseThrow();
        String systemPrompt = compiler.compile();
        String contextJson = compiler.serializeContext(buildContext(attempt, taskPackage));
        for (int cycle = 1; cycle <= MAX_GENERATION_CYCLES; cycle++) {
            String raw = generationPort.generate(systemPrompt, contextJson);
            HintGenerationDraft draft;
            try {
                draft = HintGenerationDraft.parse(raw);
            } catch (ApplyDraftException exception) {
                continue;
            }
            if (draft instanceof HintGenerationDraft.SourceGap) {
                return new GeneratedLadder.Unavailable(HintUnavailableReason.SOURCE_GAP);
            }
            HintGenerationDraft.LadderReady ladderReady = (HintGenerationDraft.LadderReady) draft;
            GateResult<HintGenerationDraft.LadderReady> gateResult = gatePipeline.validate(
                    ladderReady,
                    new HintLadderGatePolicy(gateFacts(taskPackage)),
                    GateContext.empty());
            if (gateResult.outcome() == GateOutcome.PASSED) {
                return new GeneratedLadder.Ready(HintLadder.from(attempt.attemptId(), ladderReady));
            }
        }
        return new GeneratedLadder.Unavailable(HintUnavailableReason.NODE_EXECUTION_FAILED);
    }

    private sealed interface GeneratedLadder permits GeneratedLadder.Ready, GeneratedLadder.Unavailable {

        record Ready(HintLadder ladder) implements GeneratedLadder {
        }

        record Unavailable(HintUnavailableReason reason) implements GeneratedLadder {
        }
    }

    private HintExecutionContext buildContext(TaskAttempt attempt, TaskPackage taskPackage) {
        LearnerProjection projection = taskPackage.learnerProjection();
        PrivateAssessorProjection privateFacts = taskPackage.privateAssessorProjection();
        PrivateAssessorProjection.CanonicalExpectedAnswer expected = privateFacts.canonicalExpectedAnswer();
        List<HintExecutionContext.SourcePassageView> passages = new ArrayList<>();
        for (PrivateAssessorProjection.SourceTraceEntry trace : privateFacts.sourceTrace()) {
            sourcePack.passages().stream()
                    .filter(passage -> passage.passageId().equals(trace.passageId())
                            && passage.sourceDocumentId().equals(trace.sourceDocumentId()))
                    .findFirst()
                    .ifPresent(passage -> passages.add(new HintExecutionContext.SourcePassageView(
                            passage.sourceDocumentId(), passage.sourceVersion(), passage.passageId(),
                            passage.content())));
        }
        List<Integer> exposedLevels = attempt.assistanceTrace().stream()
                .map(entry -> entry.level().level())
                .toList();
        return new HintExecutionContext(
                HintExecutionContext.SCHEMA,
                new HintExecutionContext.TaskView(projection.taskText(), projection.locale()),
                new HintExecutionContext.ExpectedAnswer(
                        expected.expression(), expected.variables(), expected.domain()),
                passages,
                exposedLevels,
                attempt.highestHintLevel() + 1,
                projection.locale());
    }

    private HintGateFacts gateFacts(TaskPackage taskPackage) {
        PrivateAssessorProjection.CanonicalExpectedAnswer expected =
                taskPackage.privateAssessorProjection().canonicalExpectedAnswer();
        List<HintGateFacts.SourceRef> approved = taskPackage.privateAssessorProjection().sourceTrace().stream()
                .map(entry -> new HintGateFacts.SourceRef(entry.sourceDocumentId(), entry.passageId()))
                .toList();
        return new HintGateFacts(expected.expression(), expected.variables(), approved);
    }
}
