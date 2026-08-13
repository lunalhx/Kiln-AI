package cn.lunalhx.ai.kilnai.domain.tool;

import cn.lunalhx.ai.kilnai.domain.skill.CapabilityGap;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolResolverTest {

    private final ToolResolver resolver = new ToolResolver();

    @Test
    void intersectionPinsCalculatorWhenAllowedAndRequired() {
        List<ToolHandle> tools = resolver.resolve(
                new ToolPermissionSet(Set.of("calculator@1")),
                Set.of("calculator@1"),
                Set.of(new ToolHandle("calculator", 1, "{\"type\":\"object\"}")),
                true
        );

        assertEquals(List.of(new ToolHandle("calculator", 1, "{\"type\":\"object\"}")), tools);
    }

    @Test
    void missingRuntimeToolIsCapabilityGap() {
        CapabilityGap gap = assertThrows(CapabilityGap.class, () -> resolver.resolve(
                new ToolPermissionSet(Set.of("calculator@1")),
                Set.of("calculator@1"),
                Set.of(),
                true
        ));

        assertTrue(gap.getMessage().contains("calculator@1"));
    }
}
