package cn.lunalhx.ai.kilnai.domain.apply.profile;

import cn.lunalhx.ai.kilnai.domain.apply.ApplyHash;
import cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleStack;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyJson;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyLearnerEvent;
import cn.lunalhx.ai.kilnai.domain.apply.model.ExplainExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.ExplainGenerationDraft;
import cn.lunalhx.ai.kilnai.domain.apply.model.ExplainTeachingArtifact;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachingProjection;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Assembles the validated teaching draft into the durable Explain teaching
 * artifact: the learner projection (principle summary, exactly one worked
 * example, and the fixed teaching Interaction Contract), the private source
 * trace with resolved versions, the Profile-derived example Fingerprint, and
 * the pinned execution trace. The generating model never owns the Fingerprint
 * or the projection boundary.
 */
public final class ExplainArtifactAssembler {

    private static final List<ApplyLearnerEvent> LEGAL_EVENTS = List.of(
            ApplyLearnerEvent.CONTINUE_REQUESTED,
            ApplyLearnerEvent.CLARIFICATION_ASKED,
            ApplyLearnerEvent.FLOW_CONTROL);

    public Optional<ExplainTeachingArtifact> assemble(
            ExplainExecutionContext context,
            ExplainGenerationDraft.TeachingReady draft,
            BundleStack stack
    ) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(draft, "draft must not be null");
        Objects.requireNonNull(stack, "stack must not be null");

        List<ExplainTeachingArtifact.SourceTraceEntry> sourceTrace = new ArrayList<>();
        for (ExplainGenerationDraft.SourceTraceEntry entry : draft.sourceTrace()) {
            Optional<ExplainExecutionContext.SourcePassage> passage = context.conceptSourcePack()
                    .passages().stream()
                    .filter(p -> p.passageId().equals(entry.passageId())
                            && p.sourceDocumentId().equals(entry.sourceDocumentId()))
                    .findFirst();
            if (passage.isEmpty()) {
                return Optional.empty();
            }
            sourceTrace.add(new ExplainTeachingArtifact.SourceTraceEntry(
                    entry.sourceDocumentId(), passage.get().sourceVersion(), entry.passageId()));
        }

        List<TeachingProjection.Step> steps = new ArrayList<>();
        for (ExplainGenerationDraft.Step step : draft.workedExample().steps()) {
            steps.add(new TeachingProjection.Step(
                    step.expression(), step.ruleReference(), step.explanation()));
        }

        String fingerprintValue = deriveFingerprint(context, draft, sourceTrace);

        ExplainTeachingArtifact artifact = new ExplainTeachingArtifact(
                UUID.randomUUID(),
                new TeachingProjection(
                        draft.principleSummary(),
                        new TeachingProjection.WorkedExample(
                                draft.workedExample().problem(),
                                steps,
                                draft.workedExample().finalResult()),
                        LEGAL_EVENTS),
                sourceTrace,
                new ExplainTeachingArtifact.ExampleFingerprint("profile", fingerprintValue),
                new ExplainTeachingArtifact.ExecutionTrace(ExplainProfile.PROFILE_ID, stack.pinnedIds()));
        return Optional.of(artifact);
    }

    private String deriveFingerprint(
            ExplainExecutionContext context,
            ExplainGenerationDraft.TeachingReady draft,
            List<ExplainTeachingArtifact.SourceTraceEntry> sourceTrace
    ) {
        List<String> stepLines = new ArrayList<>();
        for (ExplainGenerationDraft.Step step : draft.workedExample().steps()) {
            stepLines.add(String.join("|",
                    step.expression(), step.ruleReference(), step.explanation()));
        }
        String raw = String.join("|",
                draft.workedExample().problem(),
                String.join("\n", stepLines),
                draft.workedExample().finalResult(),
                ApplyJson.write(sourceTrace),
                context.conceptContract().id());
        return ApplyHash.sha256Hex(raw);
    }
}
