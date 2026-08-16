package cn.lunalhx.ai.kilnai.domain.apply.profile;

import cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleStack;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyLearnerEvent;
import cn.lunalhx.ai.kilnai.domain.apply.model.LearnerProjection;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackGenerationDraft;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackTaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackTaskPackage.AnchorReference;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackTaskPackage.ExecutionTrace;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackTaskPackage.RubricDimension;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackTaskPackage.SourceTraceEntry;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackTaskPackage.TeachBackPrivateProjection;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Assembles the closed {@code teach_back_generation/v1} task-ready draft into
 * a validated {@code teach_back_task_package/v1} Task Package: one required
 * short-text answer field, no derivative field, the fixed Teach-back
 * Interaction Contract (Answer Submitted, Clarification Asked, and Flow
 * Control Requested — never Hint), the three-dimensional Rubric mapping, the
 * grounded source trace, the anchor reference, and the pinned execution
 * trace. The private projection never carries a verbatim expected
 * explanation.
 */
public final class TeachBackTaskAssembler {

    public TeachBackTaskPackage assemble(
            TeachBackGenerationDraft.TaskReady draft,
            BundleStack stack,
            String learnerLocale
    ) {
        Objects.requireNonNull(draft, "draft must not be null");
        Objects.requireNonNull(stack, "stack must not be null");
        UUID anchorId = UUID.fromString(draft.anchorReference().anchorId());
        List<RubricDimension> rubricMapping = draft.rubricMapping().stream()
                .map(entry -> new RubricDimension(entry.dimension(), entry.masteryCriterion()))
                .toList();
        List<SourceTraceEntry> sourceTrace = draft.sourceTrace().stream()
                .map(entry -> new SourceTraceEntry(entry.sourceDocumentId(), entry.passageId()))
                .toList();
        LearnerProjection projection = new LearnerProjection(
                learnerLocale,
                draft.learnerPrompt(),
                List.of(new LearnerProjection.AnswerField(
                        "short_text_response", "简短回答", "short_text", null, null, true)),
                List.of(ApplyLearnerEvent.ANSWER_SUBMITTED, ApplyLearnerEvent.CLARIFICATION_ASKED,
                        ApplyLearnerEvent.FLOW_CONTROL),
                new LearnerProjection.SubmissionRule(1));
        return new TeachBackTaskPackage(
                TeachBackTaskPackage.SCHEMA,
                UUID.randomUUID(),
                AttemptPurpose.PRACTICE,
                projection,
                new TeachBackPrivateProjection(
                        rubricMapping,
                        sourceTrace,
                        new AnchorReference(anchorId, draft.anchorReference().anchorKind()),
                        new ExecutionTrace(TeachBackProfile.PROFILE_ID, stack.pinnedIds())));
    }
}
