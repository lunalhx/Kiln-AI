package cn.lunalhx.ai.kilnai.domain.skill;

import cn.lunalhx.ai.kilnai.domain.pedagogy.model.valobj.TeachingAction;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillResolverTest {

    private final SkillResolver resolver = new SkillResolver();

    @Test
    void resolvePinsDefaultActionAndRequiredCapability() {
        SkillStack stack = resolver.resolve(
                TeachingAction.APPLY,
                Set.of("quantitative"),
                Set.of(),
                List.of(explainAction(), applyAction(), quantitativeCapability())
        );

        assertEquals("apply.worked-example@1", stack.actionSkill().id());
        assertEquals("capability.quantitative@1", stack.capabilitySkills().get(0).id());
        assertEquals(1, stack.actionSkill().version());
    }

    @Test
    void equalPriorityCollisionIsRejected() {
        SkillManifest duplicate = new SkillManifest(
                "apply.alt", 1, SkillSlot.ACTION, TeachingAction.APPLY, Set.of(), Set.of(),
                List.of(), List.of(), Set.of(), true, 10
        );

        CapabilityGap gap = assertThrows(CapabilityGap.class, () -> resolver.resolve(
                TeachingAction.APPLY,
                Set.of(),
                Set.of(),
                List.of(applyAction(), duplicate)
        ));

        assertTrue(gap.getMessage().contains("collision"));
    }

    @Test
    void missingRequiredCapabilityReturnsGap() {
        CapabilityGap gap = assertThrows(CapabilityGap.class, () -> resolver.resolve(
                TeachingAction.APPLY,
                Set.of("quantitative"),
                Set.of(),
                List.of(applyAction())
        ));

        assertTrue(gap.getMessage().contains("quantitative"));
    }

    private SkillManifest explainAction() {
        return new SkillManifest(
                "explain.direct", 1, SkillSlot.ACTION, TeachingAction.EXPLAIN, Set.of(), Set.of(),
                List.of(), List.of(), Set.of(), true, 10
        );
    }

    private SkillManifest applyAction() {
        return new SkillManifest(
                "apply.worked-example", 1, SkillSlot.ACTION, TeachingAction.APPLY, Set.of(), Set.of("worked-example"),
                List.of(), List.of(), Set.of("calculator@1"), true, 10
        );
    }

    private SkillManifest quantitativeCapability() {
        return new SkillManifest(
                "capability.quantitative", 1, SkillSlot.REASONING, null, Set.of("quantitative"), Set.of(),
                List.of(), List.of(), Set.of("calculator@1"), false, 5
        );
    }
}
