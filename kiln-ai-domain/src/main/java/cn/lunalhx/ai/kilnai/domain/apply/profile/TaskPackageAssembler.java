package cn.lunalhx.ai.kilnai.domain.apply.profile;

import cn.lunalhx.ai.kilnai.domain.apply.ApplyHash;
import cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleStack;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyGenerationDraft;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyJson;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyLearnerEvent;
import cn.lunalhx.ai.kilnai.domain.apply.model.ExpectedExpressionNormalizer;
import cn.lunalhx.ai.kilnai.domain.apply.model.LearnerProjection;
import cn.lunalhx.ai.kilnai.domain.apply.model.PrivateAssessorFacts;
import cn.lunalhx.ai.kilnai.domain.apply.model.PrivateAssessorProjection;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class TaskPackageAssembler {

    public Optional<TaskPackage> assemble(
            ApplyExecutionContext context,
            ApplyGenerationDraft.TaskReady draft,
            BundleStack stack
    ) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(draft, "draft must not be null");
        Objects.requireNonNull(stack, "stack must not be null");

        Optional<String> canonical = ExpectedExpressionNormalizer.normalize(
                draft.privateAssessorFacts().proposedExpectedAnswer().expression(),
                context.answerRepresentationContract().variables());
        if (canonical.isEmpty()) {
            return Optional.empty();
        }

        List<PrivateAssessorProjection.SourceTraceEntry> sourceTrace = new java.util.ArrayList<>();
        for (PrivateAssessorFacts.DraftSourceTraceEntry entry : draft.privateAssessorFacts().sourceTrace()) {
            Optional<ApplyExecutionContext.SourcePassage> passage = context.conceptSourcePack().passages().stream()
                    .filter(p -> p.passageId().equals(entry.passageId())
                            && p.sourceDocumentId().equals(entry.sourceDocumentId()))
                    .findFirst();
            if (passage.isEmpty()) {
                return Optional.empty();
            }
            sourceTrace.add(new PrivateAssessorProjection.SourceTraceEntry(
                    entry.sourceDocumentId(), passage.get().sourceVersion(), entry.passageId()));
        }

        String fingerprintValue = deriveFingerprint(context, draft, canonical.get(), sourceTrace);

        TaskPackage taskPackage = new TaskPackage(
                TaskPackage.SCHEMA,
                UUID.randomUUID(),
                context.taskBlueprint().attemptPurpose(),
                learnerProjection(context, draft),
                new PrivateAssessorProjection(
                        new PrivateAssessorProjection.CanonicalExpectedAnswer(
                                canonical.get(),
                                context.answerRepresentationContract().variables(),
                                draft.privateAssessorFacts().equivalenceDeclaration().domain()),
                        draft.privateAssessorFacts().rubricMapping(),
                        sourceTrace,
                        draft.privateAssessorFacts().equivalenceDeclaration(),
                        new PrivateAssessorProjection.TaskFingerprint("profile", fingerprintValue),
                        new PrivateAssessorProjection.ExecutionTrace(
                                ApplyProfile.PROFILE_ID,
                                context.taskBlueprint().pinnedId(),
                                stack.pinnedIds()))
        );
        return Optional.of(taskPackage);
    }

    private LearnerProjection learnerProjection(
            ApplyExecutionContext context,
            ApplyGenerationDraft.TaskReady draft
    ) {
        return new LearnerProjection(
                context.learnerLocale(),
                draft.learnerTaskText(),
                List.of(
                        new LearnerProjection.AnswerField(
                                "final_derivative",
                                "f'(x)",
                                "mathematical_expression",
                                context.answerRepresentationContract().variables(),
                                context.answerRepresentationContract().acceptedInputFamilies(),
                                "required".equals(context.taskBlueprint().responseFields().finalDerivative())),
                        new LearnerProjection.AnswerField(
                                "rule_rationale",
                                "理由（可选）",
                                "short_text",
                                null,
                                null,
                                "required".equals(context.taskBlueprint().responseFields().ruleRationale()))
                ),
                List.of(
                        ApplyLearnerEvent.ANSWER_SUBMITTED,
                        ApplyLearnerEvent.PROCEDURAL_CLARIFICATION,
                        ApplyLearnerEvent.FLOW_CONTROL),
                new LearnerProjection.SubmissionRule(1)
        );
    }

    private String deriveFingerprint(
            ApplyExecutionContext context,
            ApplyGenerationDraft.TaskReady draft,
            String canonicalExpression,
            List<PrivateAssessorProjection.SourceTraceEntry> sourceTrace
    ) {
        String raw = String.join("|",
                context.taskBlueprint().attemptPurpose().wireValue(),
                context.taskBlueprint().pinnedId(),
                draft.learnerTaskText(),
                canonicalExpression,
                ApplyJson.write(draft.privateAssessorFacts().rubricMapping()),
                ApplyJson.write(sourceTrace),
                context.answerRepresentationContract().pinnedId());
        return ApplyHash.sha256Hex(raw);
    }
}
