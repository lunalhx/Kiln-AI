package cn.lunalhx.ai.kilnai.application.port;

import cn.lunalhx.ai.kilnai.domain.artifact.EvidenceCandidate;
import cn.lunalhx.ai.kilnai.domain.blackboard.LearningBlackboard;

import java.util.Map;

public interface AssessmentModelPort {

    EvidenceCandidate assess(LearningBlackboard blackboard, Map<String, Object> taskPackage, String answer, java.util.List<String> assistanceTrace);
}
