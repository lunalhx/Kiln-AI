package cn.lunalhx.ai.kilnai.domain.apply.model;

import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;

import java.util.List;
import java.util.Objects;

/**
 * A closed model contract violation. The exception message is learner-safe and
 * never carries raw model JSON, prompts, learner answers, or parser details.
 * Callers recover by the responsibility-specific policy; HTTP mapping must
 * never treat this as a provider 503.
 */
public final class ModelContractInvalidException extends ApplicationException {

    public static final String LEARNER_SAFE_MESSAGE = "当前无法完成这次评估，请稍后重试";

    private final List<String> violationCodes;

    public ModelContractInvalidException(List<String> violationCodes) {
        super(ErrorCode.MODEL_CONTRACT_INVALID, LEARNER_SAFE_MESSAGE);
        this.violationCodes = List.copyOf(Objects.requireNonNull(violationCodes, "violationCodes must not be null"));
        if (this.violationCodes.isEmpty()) {
            throw new IllegalArgumentException("violationCodes must not be empty");
        }
    }

    public List<String> violationCodes() {
        return violationCodes;
    }
}
