package cn.lunalhx.ai.kilnai.domain.artifact;

import cn.lunalhx.ai.kilnai.domain.pedagogy.model.valobj.TeachingAction;

import java.util.Set;

public record PedagogyPlan(
        String feedbackSummary,
        TeachingAction nextAction,
        String teachingIntent,
        Set<String> requiredCapabilityTags,
        Set<String> preferredStrategyTags,
        String reasonCode
) {
}
