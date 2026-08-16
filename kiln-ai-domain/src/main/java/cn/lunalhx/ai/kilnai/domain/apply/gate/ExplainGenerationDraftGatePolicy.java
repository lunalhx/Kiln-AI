package cn.lunalhx.ai.kilnai.domain.apply.gate;

import cn.lunalhx.ai.kilnai.domain.apply.model.ExplainExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.ExplainGenerationDraft;
import cn.lunalhx.ai.kilnai.domain.gate.GateContext;
import cn.lunalhx.ai.kilnai.domain.gate.GatePolicy;
import cn.lunalhx.ai.kilnai.domain.gate.GateResult;
import cn.lunalhx.ai.kilnai.domain.gate.GateViolation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The draft-level Explain gate: every worked step must map to an approved rule
 * from the Concept Contract's included scope, and every source trace entry
 * must reference an approved passage. The assembled artifact is validated
 * again by {@link ExplainGatePolicy} before delivery.
 */
public final class ExplainGenerationDraftGatePolicy implements GatePolicy<ExplainGenerationDraft.TeachingReady> {

    private final ExplainExecutionContext context;

    public ExplainGenerationDraftGatePolicy(ExplainExecutionContext context) {
        this.context = Objects.requireNonNull(context, "context must not be null");
    }

    @Override
    public GateResult<ExplainGenerationDraft.TeachingReady> evaluate(
            ExplainGenerationDraft.TeachingReady draft,
            GateContext gateContext
    ) {
        List<GateViolation> violations = new ArrayList<>();

        Set<String> approvedRules = context.conceptContract().includedScope().stream()
                .map(String::trim)
                .collect(Collectors.toSet());
        boolean rulesMapped = draft.workedExample().steps().stream()
                .allMatch(step -> approvedRules.contains(step.ruleReference().trim()));
        if (!rulesMapped) {
            violations.add(new GateViolation("explain.rule-mapping",
                    "every worked step must map to an approved rule from the included scope"));
        }

        Set<String> approvedPassageIds = context.conceptSourcePack().passages().stream()
                .map(ExplainExecutionContext.SourcePassage::passageId)
                .collect(Collectors.toSet());
        boolean grounded = draft.sourceTrace().stream()
                .allMatch(entry -> approvedPassageIds.contains(entry.passageId())
                        && context.conceptSourcePack().passages().stream().anyMatch(
                        passage -> passage.sourceDocumentId().equals(entry.sourceDocumentId())
                                && passage.passageId().equals(entry.passageId())));
        if (!grounded) {
            violations.add(new GateViolation("explain.source-ungrounded",
                    "every source trace entry must reference an approved passage"));
        }

        if (violations.isEmpty()) {
            return GateResult.passed(draft);
        }
        return GateResult.rejected(violations);
    }
}
