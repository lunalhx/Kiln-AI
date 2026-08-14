package cn.lunalhx.ai.kilnai.domain.apply.gate;

import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyLearnerEvent;
import cn.lunalhx.ai.kilnai.domain.apply.model.LearnerProjection;
import cn.lunalhx.ai.kilnai.domain.apply.model.PrivateAssessorProjection;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ApplyProfile;
import cn.lunalhx.ai.kilnai.domain.gate.GateContext;
import cn.lunalhx.ai.kilnai.domain.gate.GatePolicy;
import cn.lunalhx.ai.kilnai.domain.gate.GateResult;
import cn.lunalhx.ai.kilnai.domain.gate.GateViolation;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class ApplyTaskPackageGatePolicy implements GatePolicy<TaskPackage> {

    private static final Set<ApplyLearnerEvent> LEGAL_EVENTS = EnumSet.of(
            ApplyLearnerEvent.ANSWER_SUBMITTED,
            ApplyLearnerEvent.PROCEDURAL_CLARIFICATION,
            ApplyLearnerEvent.FLOW_CONTROL
    );

    private final ApplyExecutionContext context;
    private final List<String> pinnedStack;

    public ApplyTaskPackageGatePolicy(ApplyExecutionContext context, List<String> pinnedStack) {
        this.context = Objects.requireNonNull(context, "context must not be null");
        this.pinnedStack = List.copyOf(pinnedStack);
    }

    @Override
    public GateResult<TaskPackage> evaluate(TaskPackage candidate, GateContext gateContext) {
        List<GateViolation> violations = new ArrayList<>();

        if (!TaskPackage.SCHEMA.equals(candidate.schema()) || candidate.taskPackageId() == null) {
            violations.add(new GateViolation("package.schema", "task package schema or id is invalid"));
        }
        if (candidate.attemptPurpose() != context.taskBlueprint().attemptPurpose()) {
            violations.add(new GateViolation("package.purpose", "attempt purpose must match the task blueprint"));
        }

        LearnerProjection learner = candidate.learnerProjection();
        if (!context.learnerLocale().equals(learner.locale())) {
            violations.add(new GateViolation("learner.locale", "learner projection must render in the declared locale"));
        }
        if (learner.taskText() == null || learner.taskText().isBlank()) {
            violations.add(new GateViolation("learner.task-text", "learner task text is required"));
        }
        validateAnswerFields(learner, violations);
        if (!Set.copyOf(learner.allowedEvents()).equals(LEGAL_EVENTS)) {
            violations.add(new GateViolation("interaction.events",
                    "allowed events must be exactly answer_submitted, procedural_clarification, flow_control"));
        }
        if (learner.submissionRule() == null || learner.submissionRule().maxFormalSubmissions() != 1) {
            violations.add(new GateViolation("interaction.submission",
                    "exactly one formal submission must be permitted"));
        }

        validatePrivateProjection(candidate.privateAssessorProjection(), violations);

        String fingerprint = candidate.privateAssessorProjection().taskFingerprint().value();
        if (context.noveltyExclusions().exposedTaskFingerprints().contains(fingerprint)) {
            violations.add(new GateViolation("novelty.task-fingerprint",
                    "candidate re-exposes a previously exposed task fingerprint"));
        }
        String solutionFingerprint = candidate.privateAssessorProjection().solutionFingerprint().value();
        if (context.noveltyExclusions().exposedSolutionFingerprints().contains(solutionFingerprint)) {
            violations.add(new GateViolation("novelty.solution-fingerprint",
                    "candidate re-exposes a previously exposed solution fingerprint"));
        }

        List<String> privateSecrets = privateSecrets(candidate.privateAssessorProjection());
        if (privateSecrets.stream().anyMatch(learner.taskText()::contains)) {
            violations.add(new GateViolation("visibility.leak", "private assessor facts leaked into learner text"));
        }

        if (violations.isEmpty()) {
            return GateResult.passed(candidate);
        }
        return GateResult.rejected(violations);
    }

    private void validateAnswerFields(LearnerProjection learner, List<GateViolation> violations) {
        List<LearnerProjection.AnswerField> fields = learner.answerFields();
        LearnerProjection.AnswerField derivative = fields.stream()
                .filter(field -> "final_derivative".equals(field.id()))
                .findFirst().orElse(null);
        LearnerProjection.AnswerField rationale = fields.stream()
                .filter(field -> "rule_rationale".equals(field.id()))
                .findFirst().orElse(null);
        if (derivative == null
                || !"mathematical_expression".equals(derivative.kind())
                || !derivative.required()
                || !Set.copyOf(derivative.variables()).equals(Set.copyOf(context.answerRepresentationContract().variables()))
                || !Set.copyOf(derivative.acceptedInputFamilies()).equals(
                Set.copyOf(context.answerRepresentationContract().acceptedInputFamilies()))) {
            violations.add(new GateViolation("learner.answer-fields",
                    "the required final-derivative field must match the representation contract"));
        }
        if (rationale == null
                || !"short_text".equals(rationale.kind())
                || rationale.required()) {
            violations.add(new GateViolation("learner.answer-fields",
                    "the optional rule-rationale field must be an optional short text"));
        }
    }

    private void validatePrivateProjection(
            PrivateAssessorProjection privateProjection,
            List<GateViolation> violations
    ) {
        if (privateProjection.canonicalExpectedAnswer() == null
                || privateProjection.canonicalExpectedAnswer().expression().isBlank()) {
            violations.add(new GateViolation("private.expected-answer", "a canonical expected answer is required"));
        }
        Set<String> requiredCriterionIds = context.masteryRubric().criteria().stream()
                .map(ApplyExecutionContext.RubricCriterion::id)
                .collect(java.util.stream.Collectors.toSet());
        Set<String> mappedCriterionIds = privateProjection.rubricMapping().stream()
                .map(cn.lunalhx.ai.kilnai.domain.apply.model.PrivateAssessorFacts.RubricMapping::masteryCriterionId)
                .collect(java.util.stream.Collectors.toSet());
        if (!mappedCriterionIds.containsAll(requiredCriterionIds)) {
            violations.add(new GateViolation("private.rubric-mapping",
                    "rubric mapping must cover every required mastery criterion"));
        }
        if (privateProjection.sourceTrace() == null || privateProjection.sourceTrace().isEmpty()) {
            violations.add(new GateViolation("private.source-trace", "a source trace is required"));
        } else {
            boolean grounded = privateProjection.sourceTrace().stream()
                    .allMatch(entry -> context.conceptSourcePack().passages().stream()
                            .anyMatch(passage -> passage.sourceDocumentId().equals(entry.sourceDocumentId())
                                    && passage.passageId().equals(entry.passageId())));
            if (!grounded) {
                violations.add(new GateViolation("private.source-trace",
                        "every source trace entry must reference an approved passage"));
            }
        }
        if (privateProjection.taskFingerprint() == null
                || !"profile".equals(privateProjection.taskFingerprint().derivedBy())
                || privateProjection.taskFingerprint().value().isBlank()) {
            violations.add(new GateViolation("private.fingerprint",
                    "the profile-derived task fingerprint is required"));
        }
        if (privateProjection.solutionFingerprint() == null
                || !"profile".equals(privateProjection.solutionFingerprint().derivedBy())
                || privateProjection.solutionFingerprint().value().isBlank()) {
            violations.add(new GateViolation("private.solution-fingerprint",
                    "the profile-derived solution fingerprint is required"));
        }
        if (privateProjection.executionTrace() == null
                || !ApplyProfile.PROFILE_ID.equals(privateProjection.executionTrace().profile())
                || !context.taskBlueprint().pinnedId().equals(privateProjection.executionTrace().taskBlueprint())
                || !privateProjection.executionTrace().skillStack().equals(pinnedStack)) {
            violations.add(new GateViolation("private.execution-trace",
                    "the execution trace must pin the profile, blueprint, and frozen skill stack"));
        }
    }

    private List<String> privateSecrets(PrivateAssessorProjection privateProjection) {
        List<String> secrets = new ArrayList<>();
        secrets.add(privateProjection.canonicalExpectedAnswer().expression());
        privateProjection.sourceTrace().forEach(entry -> {
            secrets.add(entry.sourceDocumentId());
            secrets.add(entry.passageId());
        });
        secrets.add(privateProjection.taskFingerprint().value());
        secrets.add(privateProjection.solutionFingerprint().value());
        secrets.add(context.taskBlueprint().pinnedId());
        secrets.addAll(context.conceptSourcePack().passages().stream()
                .map(ApplyExecutionContext.SourcePassage::passageId).toList());
        secrets.addAll(pinnedStack);
        return secrets;
    }
}
