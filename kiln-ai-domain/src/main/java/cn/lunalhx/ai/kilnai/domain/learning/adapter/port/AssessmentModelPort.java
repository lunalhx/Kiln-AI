package cn.lunalhx.ai.kilnai.domain.learning.adapter.port;

import cn.lunalhx.ai.kilnai.domain.artifact.EvidenceCandidate;
import cn.lunalhx.ai.kilnai.domain.learning.model.AssessmentContextView;

public interface AssessmentModelPort {

    EvidenceCandidate assess(AssessmentContextView context, String compiledPrompt);
}
