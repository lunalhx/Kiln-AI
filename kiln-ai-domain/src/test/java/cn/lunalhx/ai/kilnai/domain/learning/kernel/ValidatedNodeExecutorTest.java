package cn.lunalhx.ai.kilnai.domain.learning.kernel;

import cn.lunalhx.ai.kilnai.domain.gate.GateContext;
import cn.lunalhx.ai.kilnai.domain.gate.GateOutcome;
import cn.lunalhx.ai.kilnai.domain.gate.GatePolicy;
import cn.lunalhx.ai.kilnai.domain.gate.GateResult;
import cn.lunalhx.ai.kilnai.domain.gate.GateViolation;
import cn.lunalhx.ai.kilnai.domain.gate.TypedArtifactGatePipeline;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValidatedNodeExecutorTest {

    @Test
    void allowsAtMostOneRepair() {
        CountingPolicy policy = new CountingPolicy();
        ValidatedNodeExecutor executor = new ValidatedNodeExecutor(new TypedArtifactGatePipeline());

        GateResult<String> result = executor.execute("draft", policy, GateContext.empty(), (candidate, violations) -> "repaired");

        assertEquals(GateOutcome.PASSED, result.outcome());
        assertEquals("repaired", result.artifact());
        assertEquals(2, policy.invocations);
    }

    private static final class CountingPolicy implements GatePolicy<String> {
        private int invocations;

        @Override
        public GateResult<String> evaluate(String candidate, GateContext context) {
            invocations++;
            if ("repaired".equals(candidate)) {
                return GateResult.passed(candidate);
            }
            return GateResult.repairable(List.of(new GateViolation("repair.once", "needs repair")));
        }
    }
}
