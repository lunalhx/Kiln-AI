package cn.lunalhx.ai.kilnai.domain.apply.gate;

import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyGenerationDraft;
import cn.lunalhx.ai.kilnai.domain.apply.model.PrivateAssessorFacts;
import cn.lunalhx.ai.kilnai.domain.gate.GateContext;
import cn.lunalhx.ai.kilnai.domain.gate.GatePolicy;
import cn.lunalhx.ai.kilnai.domain.gate.GateResult;
import cn.lunalhx.ai.kilnai.domain.gate.GateViolation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class ApplyGenerationDraftGatePolicy implements GatePolicy<ApplyGenerationDraft.TaskReady> {

    private final ApplyExecutionContext context;

    public ApplyGenerationDraftGatePolicy(ApplyExecutionContext context) {
        this.context = Objects.requireNonNull(context, "context must not be null");
    }

    @Override
    public GateResult<ApplyGenerationDraft.TaskReady> evaluate(
            ApplyGenerationDraft.TaskReady draft,
            GateContext gateContext
    ) {
        List<GateViolation> violations = new ArrayList<>();
        PrivateAssessorFacts facts = draft.privateAssessorFacts();

        Set<String> requiredCriterionIds = context.masteryRubric().criteria().stream()
                .map(ApplyExecutionContext.RubricCriterion::id)
                .collect(java.util.stream.Collectors.toSet());
        Set<String> mappedCriterionIds = facts.rubricMapping().stream()
                .map(PrivateAssessorFacts.RubricMapping::masteryCriterionId)
                .collect(java.util.stream.Collectors.toSet());
        if (!mappedCriterionIds.containsAll(requiredCriterionIds)) {
            violations.add(new GateViolation("rubric.unmapped",
                    "rubric mapping must cover every required mastery criterion"));
        }

        Set<String> approvedPassageIds = context.conceptSourcePack().passages().stream()
                .map(ApplyExecutionContext.SourcePassage::passageId)
                .collect(java.util.stream.Collectors.toSet());
        boolean grounded = facts.sourceTrace().stream()
                .allMatch(entry -> approvedPassageIds.contains(entry.passageId())
                        && context.conceptSourcePack().passages().stream().anyMatch(
                        passage -> passage.sourceDocumentId().equals(entry.sourceDocumentId())
                                && passage.passageId().equals(entry.passageId())));
        if (!grounded) {
            violations.add(new GateViolation("source.ungrounded",
                    "every source trace entry must reference an approved passage"));
        }

        Set<String> contractVariables = Set.copyOf(context.answerRepresentationContract().variables());
        boolean variablesAllowed = !facts.equivalenceDeclaration().variables().isEmpty()
                && contractVariables.containsAll(facts.equivalenceDeclaration().variables());
        if (!variablesAllowed) {
            violations.add(new GateViolation("equivalence.variables",
                    "equivalence declaration variables must be within the representation contract"));
        }

        if (violations.isEmpty()) {
            return GateResult.passed(draft);
        }
        return GateResult.rejected(violations);
    }
}
