package cn.lunalhx.ai.kilnai.domain.apply.gate;

import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyLearnerEvent;
import cn.lunalhx.ai.kilnai.domain.apply.model.ExplainExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.ExplainTeachingArtifact;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachingProjection;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ExplainProfile;
import cn.lunalhx.ai.kilnai.domain.gate.GateContext;
import cn.lunalhx.ai.kilnai.domain.gate.GatePolicy;
import cn.lunalhx.ai.kilnai.domain.gate.GateResult;
import cn.lunalhx.ai.kilnai.domain.gate.GateViolation;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The Explain Output Gate on the assembled teaching artifact: contract
 * closure, one-example cardinality and completeness, rule mapping, source
 * grounding, novelty against every exposed example Fingerprint, the fixed
 * teaching Interaction Contract, the pinned execution trace, and the
 * learner/private visibility boundary. No model Task Verifier runs for
 * Explain.
 */
public final class ExplainGatePolicy implements GatePolicy<ExplainTeachingArtifact> {

    private static final Set<ApplyLearnerEvent> LEGAL_EVENTS = EnumSet.of(
            ApplyLearnerEvent.CONTINUE_REQUESTED,
            ApplyLearnerEvent.CLARIFICATION_ASKED,
            ApplyLearnerEvent.FLOW_CONTROL);

    private final ExplainExecutionContext context;
    private final List<String> pinnedStack;

    public ExplainGatePolicy(ExplainExecutionContext context, List<String> pinnedStack) {
        this.context = Objects.requireNonNull(context, "context must not be null");
        this.pinnedStack = List.copyOf(pinnedStack);
    }

    @Override
    public GateResult<ExplainTeachingArtifact> evaluate(
            ExplainTeachingArtifact candidate,
            GateContext gateContext
    ) {
        List<GateViolation> violations = new ArrayList<>();

        TeachingProjection projection = candidate.learnerProjection();
        if (projection.principleSummary() == null || projection.principleSummary().isBlank()) {
            violations.add(new GateViolation("explain.principle", "a principle summary is required"));
        }
        TeachingProjection.WorkedExample example = projection.workedExample();
        if (example.problem() == null || example.problem().isBlank()) {
            violations.add(new GateViolation("explain.example.problem", "the worked example problem is required"));
        }
        if (example.steps() == null || example.steps().isEmpty()) {
            violations.add(new GateViolation("explain.example.steps",
                    "exactly one complete worked example with ordered steps is required"));
        }
        boolean stepsComplete = example.steps().stream().allMatch(step ->
                step.expression() != null && !step.expression().isBlank()
                        && step.ruleReference() != null && !step.ruleReference().isBlank()
                        && step.explanation() != null && !step.explanation().isBlank());
        if (!stepsComplete) {
            violations.add(new GateViolation("explain.example.steps",
                    "every worked step must carry expression, rule reference, and explanation"));
        }
        if (example.finalResult() == null || example.finalResult().isBlank()) {
            violations.add(new GateViolation("explain.example.final-result",
                    "the worked example final result is required"));
        }
        if (!Set.copyOf(projection.allowedEvents()).equals(LEGAL_EVENTS)) {
            violations.add(new GateViolation("explain.interaction.events",
                    "allowed events must be exactly continue_requested, clarification_asked, flow_control"));
        }

        Set<String> approvedRules = context.conceptContract().includedScope().stream()
                .map(String::trim)
                .collect(Collectors.toSet());
        boolean rulesMapped = example.steps().stream()
                .allMatch(step -> approvedRules.contains(step.ruleReference().trim()));
        if (!rulesMapped) {
            violations.add(new GateViolation("explain.rule-mapping",
                    "every worked step must map to an approved rule from the included scope"));
        }

        boolean grounded = candidate.sourceTrace().stream()
                .allMatch(entry -> context.conceptSourcePack().passages().stream()
                        .anyMatch(passage -> passage.sourceDocumentId().equals(entry.sourceDocumentId())
                                && passage.passageId().equals(entry.passageId())));
        if (!grounded) {
            violations.add(new GateViolation("explain.source-ungrounded",
                    "every source trace entry must reference an approved passage"));
        }

        String fingerprint = candidate.exampleFingerprint().value();
        if (context.noveltyExclusions().exposedExampleFingerprints().contains(fingerprint)
                || context.noveltyExclusions().exposedHintLadderFingerprints().contains(fingerprint)
                || context.noveltyExclusions().exposedRevealedSolutionFingerprints().contains(fingerprint)) {
            violations.add(new GateViolation("explain.novelty.example-fingerprint",
                    "candidate re-exposes a previously exposed example fingerprint"));
        }

        if (candidate.executionTrace() == null
                || !ExplainProfile.PROFILE_ID.equals(candidate.executionTrace().profile())
                || !candidate.executionTrace().skillStack().equals(pinnedStack)) {
            violations.add(new GateViolation("explain.execution-trace",
                    "the execution trace must pin the profile and frozen skill stack"));
        }

        List<String> privateSecrets = privateSecrets(candidate);
        String learnerText = learnerText(projection);
        if (privateSecrets.stream().anyMatch(learnerText::contains)) {
            violations.add(new GateViolation("explain.visibility.leak",
                    "private facts leaked into learner-visible teaching content"));
        }

        if (violations.isEmpty()) {
            return GateResult.passed(candidate);
        }
        return GateResult.rejected(violations);
    }

    private String learnerText(TeachingProjection projection) {
        StringBuilder text = new StringBuilder();
        text.append(projection.principleSummary());
        TeachingProjection.WorkedExample example = projection.workedExample();
        if (example != null) {
            text.append(example.problem());
            example.steps().forEach(step ->
                    text.append(step.expression()).append(step.ruleReference()).append(step.explanation()));
            text.append(example.finalResult());
        }
        return text.toString();
    }

    private List<String> privateSecrets(ExplainTeachingArtifact candidate) {
        List<String> secrets = new ArrayList<>();
        candidate.sourceTrace().forEach(entry -> {
            secrets.add(entry.sourceDocumentId());
            secrets.add(entry.passageId());
        });
        secrets.add(candidate.exampleFingerprint().value());
        context.conceptSourcePack().passages().stream()
                .map(ExplainExecutionContext.SourcePassage::passageId)
                .forEach(secrets::add);
        secrets.addAll(pinnedStack);
        return secrets;
    }
}
