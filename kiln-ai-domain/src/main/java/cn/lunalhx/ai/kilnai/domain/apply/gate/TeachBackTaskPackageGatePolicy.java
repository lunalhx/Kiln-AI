package cn.lunalhx.ai.kilnai.domain.apply.gate;

import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyLearnerEvent;
import cn.lunalhx.ai.kilnai.domain.apply.model.LearnerProjection;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelExecution;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackTaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.profile.TeachBackProfile;
import cn.lunalhx.ai.kilnai.domain.gate.GateContext;
import cn.lunalhx.ai.kilnai.domain.gate.GatePolicy;
import cn.lunalhx.ai.kilnai.domain.gate.GateResult;
import cn.lunalhx.ai.kilnai.domain.gate.GateViolation;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The Teach-back Output Gate on the assembled task package: contract closure,
 * exactly one required short-text answer field and no derivative field, the
 * fixed Teach-back Interaction Contract (Answer Submitted, Clarification
 * Asked, and Flow Control Requested — never Hint, per ADR-0065), the
 * three-dimensional Rubric mapping, source grounding against the anchor's
 * source trace, the anchor reference pinned to the supplied anchor, the
 * pinned execution trace, and the learner/private visibility boundary. No
 * verbatim expected explanation can ever exist to leak: the draft contract
 * rejects it and the private projection never carries one.
 */
public final class TeachBackTaskPackageGatePolicy implements GatePolicy<TeachBackTaskPackage> {

    private static final Set<ApplyLearnerEvent> LEGAL_EVENTS = EnumSet.of(
            ApplyLearnerEvent.ANSWER_SUBMITTED,
            ApplyLearnerEvent.CLARIFICATION_ASKED,
            ApplyLearnerEvent.FLOW_CONTROL);

    private final TeachBackExecutionContext context;
    private final List<String> pinnedStack;
    private final ModelProfile profile;

    public TeachBackTaskPackageGatePolicy(
            TeachBackExecutionContext context,
            List<String> pinnedStack,
            ModelProfile profile
    ) {
        this.context = Objects.requireNonNull(context, "context must not be null");
        this.pinnedStack = List.copyOf(pinnedStack);
        this.profile = Objects.requireNonNull(profile, "profile must not be null");
    }

    @Override
    public GateResult<TeachBackTaskPackage> evaluate(
            TeachBackTaskPackage candidate,
            GateContext gateContext
    ) {
        List<GateViolation> violations = new ArrayList<>();

        LearnerProjection projection = candidate.learnerProjection();
        if (projection.taskText() == null || projection.taskText().isBlank()) {
            violations.add(new GateViolation("teach-back.task.prompt", "a learner prompt is required"));
        }
        boolean exactlyOneShortTextField = projection.answerFields().size() == 1
                && projection.answerFields().get(0).required()
                && "short_text".equals(projection.answerFields().get(0).kind());
        if (!exactlyOneShortTextField) {
            violations.add(new GateViolation("teach-back.task.answer-field",
                    "exactly one required short-text answer field and no derivative field is required"));
        }
        if (!Set.copyOf(projection.allowedEvents()).equals(LEGAL_EVENTS)) {
            violations.add(new GateViolation("teach-back.interaction.events",
                    "allowed events must be exactly answer_submitted, clarification_asked, flow_control"));
        }
        if (projection.submissionRule() == null || projection.submissionRule().maxFormalSubmissions() != 1) {
            violations.add(new GateViolation("teach-back.task.submission-rule",
                    "exactly one formal submission is required"));
        }

        TeachBackTaskPackage.TeachBackPrivateProjection privateFacts = candidate.privateProjection();
        Set<String> dimensions = privateFacts.rubricMapping().stream()
                .map(TeachBackTaskPackage.RubricDimension::dimension)
                .collect(Collectors.toSet());
        if (!dimensions.equals(new HashSet<>(TeachBackProfile.RUBRIC_DIMENSIONS))) {
            violations.add(new GateViolation("teach-back.rubric.dimensions",
                    "the rubric mapping must cover exactly rule_identification, "
                            + "applicability_explanation, and steps_result_coherence"));
        }
        boolean criteriaMapped = privateFacts.rubricMapping().stream()
                .allMatch(entry -> context.masteryRubric().criteria().stream()
                        .anyMatch(criterion -> criterion.id().equals(entry.masteryCriterion())));
        if (!criteriaMapped) {
            violations.add(new GateViolation("teach-back.rubric.criteria",
                    "every rubric dimension must map to a Mastery Rubric criterion"));
        }

        Set<String> anchorSources = context.anchor().sourceTrace().stream()
                .map(ref -> ref.sourceDocumentId() + ":" + ref.passageId())
                .collect(Collectors.toSet());
        boolean grounded = privateFacts.sourceTrace().stream()
                .allMatch(entry -> anchorSources.contains(entry.sourceDocumentId() + ":" + entry.passageId()));
        if (!grounded) {
            violations.add(new GateViolation("teach-back.source-ungrounded",
                    "every source trace entry must reference the anchor's source trace"));
        }

        if (!Objects.equals(privateFacts.anchorReference().anchorId(), context.anchor().anchorId())
                || !privateFacts.anchorReference().anchorKind().equals(context.anchor().anchorKind())) {
            violations.add(new GateViolation("teach-back.anchor-reference",
                    "the anchor reference must match the supplied eligible anchor"));
        }

        ModelExecution model = candidate.privateProjection().executionTrace() == null
                ? null
                : candidate.privateProjection().executionTrace().model();
        if (candidate.privateProjection().executionTrace() == null
                || !TeachBackProfile.PROFILE_ID.equals(candidate.privateProjection().executionTrace().profile())
                || !candidate.privateProjection().executionTrace().skillStack().equals(pinnedStack)
                || model == null
                || !model.usesFrozenProfile(profile)) {
            violations.add(new GateViolation("teach-back.execution-trace",
                    "the execution trace must pin the profile, frozen skill stack, and frozen model runtime"));
        }

        List<String> privateSecrets = privateSecrets(candidate);
        String learnerText = projection.taskText();
        if (privateSecrets.stream().anyMatch(learnerText::contains)) {
            violations.add(new GateViolation("teach-back.visibility.leak",
                    "private facts leaked into learner-visible task text"));
        }

        if (violations.isEmpty()) {
            return GateResult.passed(candidate);
        }
        return GateResult.rejected(violations);
    }

    private List<String> privateSecrets(TeachBackTaskPackage candidate) {
        List<String> secrets = new ArrayList<>();
        candidate.privateProjection().sourceTrace().forEach(entry -> {
            secrets.add(entry.sourceDocumentId());
            secrets.add(entry.passageId());
        });
        context.anchor().sourceTrace().stream()
                .map(TeachBackExecutionContext.SourceTraceRef::passageId)
                .forEach(secrets::add);
        secrets.add(context.anchor().anchorId().toString());
        secrets.addAll(pinnedStack);
        return secrets;
    }
}
