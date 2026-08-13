package cn.lunalhx.ai.kilnai.domain.gate;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TypedArtifactGatePipelineTest {

    private final TypedArtifactGatePipeline pipeline = new TypedArtifactGatePipeline();

    @Test
    void passedPolicyReturnsTheCandidate() {
        String candidate = "ok";
        GateResult<String> result = pipeline.validate(candidate, (value, context) -> GateResult.passed(value), GateContext.empty());

        assertEquals(GateOutcome.PASSED, result.outcome());
        assertEquals("ok", result.artifact());
    }

    @Test
    void repairablePolicySurfacesViolationsWithoutMutatingState() {
        GateResult<String> result = pipeline.validate(
                "draft",
                (value, context) -> GateResult.repairable(List.of(new GateViolation("missing.source", "source identity required"))),
                GateContext.empty()
        );

        assertEquals(GateOutcome.REPAIRABLE, result.outcome());
        assertEquals("missing.source", result.violations().get(0).code());
    }

    @Test
    void rejectedPolicyLeavesNoAcceptedArtifact() {
        GateResult<String> result = pipeline.validate(
                "bad",
                (value, context) -> GateResult.rejected(List.of(new GateViolation("illegal.action", "action is not legal"))),
                GateContext.empty()
        );

        assertEquals(GateOutcome.REJECTED, result.outcome());
        assertThrows(IllegalStateException.class, result::artifact);
    }
}
