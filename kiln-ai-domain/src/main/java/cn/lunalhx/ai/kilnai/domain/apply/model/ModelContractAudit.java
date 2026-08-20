package cn.lunalhx.ai.kilnai.domain.apply.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Durable audit of one model-boundary failure. It retains only identity,
 * responsibility, normalized violation codes, repair count, correlation ID,
 * and provider-health category — never raw invalid JSON, prompts, or learner
 * responses.
 */
public record ModelContractAudit(
        UUID flowId,
        UUID attemptId,
        UUID taskPackageId,
        String responsibility,
        List<String> violationCodes,
        int repairCount,
        String correlationId,
        String providerCategory
) {

    public static final String PROVIDER_CATEGORY = "MODEL_CONTRACT_INVALID";
    public static final String MODEL_CONFIGURATION_INVALID = "MODEL_CONFIGURATION_INVALID";
    public static final String MODEL_PROVIDER_UNAVAILABLE = "MODEL_PROVIDER_UNAVAILABLE";
    public static final String TECHNICAL_FAILURE = "technical_failure";

    public static final String ASSESSMENT = "assessment";
    public static final String RESPONSE_VERIFICATION = "response_verification";
    public static final String RATIONALE_ASSESSMENT = "rationale_assessment";
    public static final String TEACH_BACK_ASSESSMENT = "teach_back_assessment";
    public static final String TASK_VERIFICATION = "task_verification";
    public static final String TEACH_BACK_TASK_VERIFICATION = "teach_back_task_verification";
    public static final String CLARIFICATION = "clarification";

    public ModelContractAudit {
        Objects.requireNonNull(responsibility, "responsibility must not be null");
        Objects.requireNonNull(violationCodes, "violationCodes must not be null");
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        Objects.requireNonNull(providerCategory, "providerCategory must not be null");
        violationCodes = List.copyOf(violationCodes);
        if (violationCodes.isEmpty()) {
            throw new IllegalArgumentException("violationCodes must not be empty");
        }
        if (repairCount < 0) {
            throw new IllegalArgumentException("repairCount must not be negative: " + repairCount);
        }
        if (flowId == null && attemptId == null && taskPackageId == null) {
            throw new IllegalArgumentException("audit must carry a Flow, Attempt, or Task Package identity");
        }
        if (correlationId.isBlank()) {
            throw new IllegalArgumentException("correlationId must not be blank");
        }
    }
}
