package cn.lunalhx.ai.kilnai.domain.learning.kernel;

import cn.lunalhx.ai.kilnai.domain.gate.GateContext;
import cn.lunalhx.ai.kilnai.domain.gate.GateOutcome;
import cn.lunalhx.ai.kilnai.domain.gate.GatePolicy;
import cn.lunalhx.ai.kilnai.domain.gate.GateResult;
import cn.lunalhx.ai.kilnai.domain.gate.TypedArtifactGatePipeline;

import java.util.Objects;
import java.util.function.Function;

public final class ValidatedNodeExecutor {

    private final TypedArtifactGatePipeline pipeline;

    public ValidatedNodeExecutor(TypedArtifactGatePipeline pipeline) {
        this.pipeline = Objects.requireNonNull(pipeline, "pipeline must not be null");
    }

    public <T> GateResult<T> execute(T candidate, GatePolicy<T> policy, GateContext context, Function<T, T> repair) {
        GateResult<T> first = pipeline.validate(candidate, policy, context);
        if (first.outcome() != GateOutcome.REPAIRABLE) {
            return first;
        }
        T repaired = repair.apply(candidate);
        return pipeline.validate(repaired, policy, context);
    }
}
