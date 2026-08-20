package cn.lunalhx.ai.kilnai.domain.apply.profile;

import cn.lunalhx.ai.kilnai.domain.apply.bundle.EvaluationBundleStack;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;
import cn.lunalhx.ai.kilnai.domain.apply.model.RationaleEvaluationContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.RationaleEvaluationResult;
import cn.lunalhx.ai.kilnai.domain.apply.port.RationaleAssessmentPort;
import cn.lunalhx.ai.kilnai.domain.skill.CapabilityGap;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;

import java.util.List;
import java.util.Objects;

/**
 * Executes one bounded, state-free Rationale Assessment invocation.
 */
public final class RationaleEvaluationProfileExecutor {

    private final EvaluationBundleStack stack;
    private final RationaleAssessmentPort assessmentPort;
    private final RationaleEvaluationPromptCompiler compiler;
    private final String profileSystemPrompt;

    public RationaleEvaluationProfileExecutor(
            EvaluationBundleStack stack,
            RationaleAssessmentPort assessmentPort
    ) {
        this(stack, assessmentPort, RationaleEvaluationProfile.BASE_SYSTEM_PROMPT);
    }

    public RationaleEvaluationProfileExecutor(
            EvaluationBundleStack stack,
            RationaleAssessmentPort assessmentPort,
            String profileSystemPrompt
    ) {
        this.stack = Objects.requireNonNull(stack, "stack must not be null");
        this.assessmentPort = Objects.requireNonNull(assessmentPort, "assessmentPort must not be null");
        this.compiler = new RationaleEvaluationPromptCompiler();
        this.profileSystemPrompt = Objects.requireNonNull(
                profileSystemPrompt, "profileSystemPrompt must not be null");
        if (profileSystemPrompt.isBlank()) {
            throw new IllegalArgumentException("profileSystemPrompt must not be blank");
        }
    }

    public RationaleEvaluationResult evaluate(
            ModelProfile profile,
            RationaleEvaluationContext context
    ) {
        return evaluate(profile, context, List.of());
    }

    public RationaleEvaluationResult evaluate(
            ModelProfile profile,
            RationaleEvaluationContext context,
            List<String> normalizedViolations
    ) {
        Objects.requireNonNull(profile, "profile must not be null");
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(normalizedViolations, "normalizedViolations must not be null");
        try {
            return assessmentPort.assess(
                    profile,
                    compiler.compile(stack, profileSystemPrompt, normalizedViolations),
                    compiler.serializeContext(context));
        } catch (CapabilityGap exception) {
            throw new ApplicationException(
                    ErrorCode.INVALID_ARGUMENT, "rationale evaluation profile is not configured");
        }
    }
}
