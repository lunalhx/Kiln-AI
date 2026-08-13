package cn.lunalhx.ai.kilnai.domain.artifact;

import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningResult;

import java.util.List;

public record EvidenceCandidate(
        LearningResult result,
        List<String> satisfiedCriteria,
        List<String> missingCriteria,
        String rationale
) {
}
