package cn.lunalhx.ai.kilnai.domain.gate;

import java.util.Objects;

public final class TypedArtifactGatePipeline {

    public <T> GateResult<T> validate(T candidate, GatePolicy<T> policy, GateContext context) {
        Objects.requireNonNull(candidate, "candidate must not be null");
        Objects.requireNonNull(policy, "policy must not be null");
        Objects.requireNonNull(context, "context must not be null");
        return policy.evaluate(candidate, context);
    }
}
