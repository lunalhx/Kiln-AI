package cn.lunalhx.ai.kilnai.domain.learning.model;

import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage;
import cn.lunalhx.ai.kilnai.domain.pedagogy.model.valobj.TeachingAction;

import java.util.List;
import java.util.UUID;

public record PedagogyContextView(
        UUID flowId,
        LearningStage stage,
        List<TeachingAction> legalCandidates,
        List<String> compactFeedbackFacts
) {
    public PedagogyContextView {
        legalCandidates = legalCandidates == null ? List.of() : List.copyOf(legalCandidates);
        compactFeedbackFacts = compactFeedbackFacts == null ? List.of() : List.copyOf(compactFeedbackFacts);
    }
}
