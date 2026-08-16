package cn.lunalhx.ai.kilnai.domain.apply.gate;

import cn.lunalhx.ai.kilnai.domain.apply.bundle.ReferenceBundles;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.DiagnosticApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.IndependentApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ApplyScriptData;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyGenerationDraft;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyLearnerEvent;
import cn.lunalhx.ai.kilnai.domain.apply.model.LearnerProjection;
import cn.lunalhx.ai.kilnai.domain.apply.model.PrivateAssessorFacts;
import cn.lunalhx.ai.kilnai.domain.apply.model.PrivateAssessorProjection;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.profile.TaskPackageAssembler;
import cn.lunalhx.ai.kilnai.domain.gate.GateContext;
import cn.lunalhx.ai.kilnai.domain.gate.GateOutcome;
import cn.lunalhx.ai.kilnai.domain.gate.GateResult;
import cn.lunalhx.ai.kilnai.domain.gate.TypedArtifactGatePipeline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplyTaskPackageGatePolicyTest {

    private static final List<String> PINNED_STACK = List.of(
            "apply.task-first@0.1.0",
            "reasoning.rule-application@0.1.0",
            "representation.formal-expression@0.1.0",
            "verification.structured-task-contract@0.1.0",
            "subject.calculus-notation@0.1.0"
    );

    private final TypedArtifactGatePipeline pipeline = new TypedArtifactGatePipeline();
    private ApplyExecutionContext context;
    private TaskPackage validPackage;

    @BeforeEach
    void setUp() {
        context = DiagnosticApplyFixture.diagnosticContext();
        validPackage = assemble(ApplyScriptData.taskReadyDraft());
    }

    @Test
    void aValidTaskPackagePassesTheOutputGate() {
        assertEquals(GateOutcome.PASSED, gate(validPackage).outcome());
    }

    @Test
    void leakingTheExpectedAnswerIntoLearnerTextIsRejected() {
        String leak = " 答案提示：" + validPackage.privateAssessorProjection()
                .canonicalExpectedAnswer().expression();
        assertRejected(withTaskText(validPackage, validPackage.learnerProjection().taskText() + leak));
    }

    @Test
    void leakingASourcePassageIdIntoLearnerTextIsRejected() {
        assertRejected(withTaskText(validPackage, "参考材料：" + DiagnosticApplyFixture.PASSAGE_ID));
    }

    @Test
    void anUngroundedSourceTraceIsRejected() {
        assertRejected(copy(validPackage,
                withSourceTrace(validPackage.privateAssessorProjection(),
                        new PrivateAssessorProjection.SourceTraceEntry(
                                "openstax-calculus-v1", "1.0.0", "not-an-approved-passage"))));
    }

    @Test
    void aMissingRequiredAnswerFieldIsRejected() {
        LearnerProjection withoutDerivative = withFields(validPackage.learnerProjection(),
                validPackage.learnerProjection().answerFields().stream()
                        .filter(field -> !"final_derivative".equals(field.id()))
                        .toList());
        assertRejected(copy(validPackage, withoutDerivative));
    }

    @Test
    void wrongAllowedEventsAreRejected() {
        LearnerProjection wrongEvents = withEvents(validPackage.learnerProjection(),
                List.of(ApplyLearnerEvent.ANSWER_SUBMITTED, ApplyLearnerEvent.FLOW_CONTROL));
        assertRejected(copy(validPackage, wrongEvents));
    }

    @Test
    void aModelClaimedFingerprintIsRejected() {
        assertRejected(copy(validPackage,
                withFingerprint(validPackage.privateAssessorProjection(),
                        new PrivateAssessorProjection.TaskFingerprint("model", "model-claimed-fp"))));
    }

    @Test
    void aModelClaimedSolutionFingerprintIsRejected() {
        assertRejected(copy(validPackage,
                withSolutionFingerprint(validPackage.privateAssessorProjection(),
                        new PrivateAssessorProjection.SolutionFingerprint("model", "model-claimed-sfp"))));
    }

    @Test
    void leakingASolutionFingerprintIntoLearnerTextIsRejected() {
        String leak = " 内部：" + validPackage.privateAssessorProjection().solutionFingerprint().value();
        assertRejected(withTaskText(validPackage, validPackage.learnerProjection().taskText() + leak));
    }

    @Test
    void aPackageWhoseTaskFingerprintWasAlreadyExposedIsRejected() {
        String fingerprint = validPackage.privateAssessorProjection().taskFingerprint().value();
        ApplyExecutionContext withExclusions = IndependentApplyFixture.independentContext().withNoveltyExclusions(
                new ApplyExecutionContext.NoveltyExclusions(
                        List.of(fingerprint), List.of(), List.of(), List.of(), List.of()));

        GateResult<TaskPackage> result = pipeline.validate(
                validPackage, new ApplyTaskPackageGatePolicy(withExclusions, PINNED_STACK), GateContext.empty());

        assertEquals(GateOutcome.REJECTED, result.outcome());
        assertTrue(result.violations().stream().anyMatch(v -> "novelty.task-fingerprint".equals(v.code())));
    }

    @Test
    void aPackageWhoseSolutionFingerprintWasAlreadyExposedIsRejected() {
        String fingerprint = validPackage.privateAssessorProjection().solutionFingerprint().value();
        ApplyExecutionContext withExclusions = IndependentApplyFixture.independentContext().withNoveltyExclusions(
                new ApplyExecutionContext.NoveltyExclusions(
                        List.of(), List.of(fingerprint), List.of(), List.of(), List.of()));

        GateResult<TaskPackage> result = pipeline.validate(
                validPackage, new ApplyTaskPackageGatePolicy(withExclusions, PINNED_STACK), GateContext.empty());

        assertEquals(GateOutcome.REJECTED, result.outcome());
        assertTrue(result.violations().stream().anyMatch(v -> "novelty.solution-fingerprint".equals(v.code())));
    }

    @Test
    void aPackageWhoseTaskFingerprintMatchesAnExposedHintLadderFingerprintIsRejected() {
        String fingerprint = validPackage.privateAssessorProjection().taskFingerprint().value();
        ApplyExecutionContext withExclusions = IndependentApplyFixture.independentContext().withNoveltyExclusions(
                new ApplyExecutionContext.NoveltyExclusions(
                        List.of(), List.of(), List.of(), List.of(fingerprint), List.of()));

        GateResult<TaskPackage> result = pipeline.validate(
                validPackage, new ApplyTaskPackageGatePolicy(withExclusions, PINNED_STACK), GateContext.empty());

        assertEquals(GateOutcome.REJECTED, result.outcome());
        assertTrue(result.violations().stream().anyMatch(v -> "novelty.task-fingerprint".equals(v.code())));
    }

    @Test
    void aPackageWhoseSolutionFingerprintMatchesAnExposedRevealedSolutionFingerprintIsRejected() {
        String fingerprint = validPackage.privateAssessorProjection().solutionFingerprint().value();
        ApplyExecutionContext withExclusions = IndependentApplyFixture.independentContext().withNoveltyExclusions(
                new ApplyExecutionContext.NoveltyExclusions(
                        List.of(), List.of(), List.of(), List.of(), List.of(fingerprint)));

        GateResult<TaskPackage> result = pipeline.validate(
                validPackage, new ApplyTaskPackageGatePolicy(withExclusions, PINNED_STACK), GateContext.empty());

        assertEquals(GateOutcome.REJECTED, result.outcome());
        assertTrue(result.violations().stream().anyMatch(v -> "novelty.solution-fingerprint".equals(v.code())));
    }

    @Test
    void aPackageWhoseTaskFingerprintMatchesAnExposedExampleFingerprintIsRejected() {
        String fingerprint = validPackage.privateAssessorProjection().taskFingerprint().value();
        ApplyExecutionContext withExclusions = IndependentApplyFixture.independentContext().withNoveltyExclusions(
                new ApplyExecutionContext.NoveltyExclusions(
                        List.of(), List.of(), List.of(fingerprint), List.of(), List.of()));

        GateResult<TaskPackage> result = pipeline.validate(
                validPackage, new ApplyTaskPackageGatePolicy(withExclusions, PINNED_STACK), GateContext.empty());

        assertEquals(GateOutcome.REJECTED, result.outcome());
        assertTrue(result.violations().stream().anyMatch(v -> "novelty.task-fingerprint".equals(v.code())));
    }

    @Test
    void rubricMappingMissingACriterionIsRejected() {
        assertRejected(copy(validPackage,
                withRubricMapping(validPackage.privateAssessorProjection(),
                        new PrivateAssessorFacts.RubricMapping("unrelated-criterion",
                                List.of("final_derivative")))));
    }

    @Test
    void aWrongLearnerLocaleIsRejected() {
        assertRejected(copy(validPackage, withLocale(validPackage.learnerProjection(), "en-US")));
    }

    @Test
    void multipleFormalSubmissionsAreRejected() {
        assertRejected(copy(validPackage,
                withSubmissionRule(validPackage.learnerProjection(), new LearnerProjection.SubmissionRule(2))));
    }

    @Test
    void theDraftGateRejectsUngroundedSourceTrace() {
        ApplyGenerationDraft.TaskReady draft = (ApplyGenerationDraft.TaskReady) ApplyGenerationDraft.parse(
                ApplyScriptData.taskReadyJson()
                        .replace("sec-3.3-differentiation-rules", "unapproved-passage"));
        GateResult<ApplyGenerationDraft.TaskReady> result = pipeline.validate(
                draft, new ApplyGenerationDraftGatePolicy(context), GateContext.empty());

        assertEquals(GateOutcome.REJECTED, result.outcome());
        assertTrue(result.violations().stream().anyMatch(v -> "source.ungrounded".equals(v.code())));
    }

    private TaskPackage assemble(ApplyGenerationDraft.TaskReady draft) {
        return new TaskPackageAssembler()
                .assemble(context, draft, ReferenceBundles.stack())
                .orElseThrow();
    }

    private GateResult<TaskPackage> gate(TaskPackage taskPackage) {
        return pipeline.validate(
                taskPackage, new ApplyTaskPackageGatePolicy(context, PINNED_STACK), GateContext.empty());
    }

    private void assertRejected(TaskPackage taskPackage) {
        assertEquals(GateOutcome.REJECTED, gate(taskPackage).outcome());
    }

    private TaskPackage withTaskText(TaskPackage source, String taskText) {
        return copy(source, withText(source.learnerProjection(), taskText));
    }

    private TaskPackage copy(TaskPackage source, LearnerProjection learner) {
        return copy(source, learner, source.privateAssessorProjection());
    }

    private TaskPackage copy(TaskPackage source, PrivateAssessorProjection privateProjection) {
        return copy(source, source.learnerProjection(), privateProjection);
    }

    private TaskPackage copy(TaskPackage source, LearnerProjection learner, PrivateAssessorProjection privateProjection) {
        return new TaskPackage(source.schema(), source.taskPackageId(), source.attemptPurpose(),
                learner, privateProjection);
    }

    private LearnerProjection withText(LearnerProjection source, String taskText) {
        return new LearnerProjection(source.locale(), taskText, source.answerFields(),
                source.allowedEvents(), source.submissionRule());
    }

    private LearnerProjection withLocale(LearnerProjection source, String locale) {
        return new LearnerProjection(locale, source.taskText(), source.answerFields(),
                source.allowedEvents(), source.submissionRule());
    }

    private LearnerProjection withFields(LearnerProjection source, List<LearnerProjection.AnswerField> fields) {
        return new LearnerProjection(source.locale(), source.taskText(), fields,
                source.allowedEvents(), source.submissionRule());
    }

    private LearnerProjection withEvents(LearnerProjection source, List<ApplyLearnerEvent> events) {
        return new LearnerProjection(source.locale(), source.taskText(), source.answerFields(),
                events, source.submissionRule());
    }

    private LearnerProjection withSubmissionRule(LearnerProjection source, LearnerProjection.SubmissionRule rule) {
        return new LearnerProjection(source.locale(), source.taskText(), source.answerFields(),
                source.allowedEvents(), rule);
    }

    private PrivateAssessorProjection withSourceTrace(
            PrivateAssessorProjection source,
            PrivateAssessorProjection.SourceTraceEntry entry
    ) {
        return new PrivateAssessorProjection(source.canonicalExpectedAnswer(), source.rubricMapping(),
                List.of(entry), source.equivalenceDeclaration(), source.taskFingerprint(),
                source.solutionFingerprint(), source.executionTrace());
    }

    private PrivateAssessorProjection withFingerprint(
            PrivateAssessorProjection source,
            PrivateAssessorProjection.TaskFingerprint fingerprint
    ) {
        return new PrivateAssessorProjection(source.canonicalExpectedAnswer(), source.rubricMapping(),
                source.sourceTrace(), source.equivalenceDeclaration(), fingerprint,
                source.solutionFingerprint(), source.executionTrace());
    }

    private PrivateAssessorProjection withSolutionFingerprint(
            PrivateAssessorProjection source,
            PrivateAssessorProjection.SolutionFingerprint solutionFingerprint
    ) {
        return new PrivateAssessorProjection(source.canonicalExpectedAnswer(), source.rubricMapping(),
                source.sourceTrace(), source.equivalenceDeclaration(), source.taskFingerprint(),
                solutionFingerprint, source.executionTrace());
    }

    private PrivateAssessorProjection withRubricMapping(
            PrivateAssessorProjection source,
            PrivateAssessorFacts.RubricMapping mapping
    ) {
        return new PrivateAssessorProjection(source.canonicalExpectedAnswer(), List.of(mapping),
                source.sourceTrace(), source.equivalenceDeclaration(), source.taskFingerprint(),
                source.solutionFingerprint(), source.executionTrace());
    }
}
