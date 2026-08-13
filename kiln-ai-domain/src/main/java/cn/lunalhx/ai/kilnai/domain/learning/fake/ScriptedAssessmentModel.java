package cn.lunalhx.ai.kilnai.application.fake;

import cn.lunalhx.ai.kilnai.application.port.AssessmentModelPort;
import cn.lunalhx.ai.kilnai.domain.artifact.EvidenceCandidate;
import cn.lunalhx.ai.kilnai.domain.blackboard.LearningBlackboard;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningResult;

import java.util.List;
import java.util.Map;

public final class ScriptedAssessmentModel implements AssessmentModelPort {

    @Override
    public EvidenceCandidate assess(
            LearningBlackboard blackboard,
            Map<String, Object> taskPackage,
            String answer,
            List<String> assistanceTrace
    ) {
        if (taskPackage.containsKey("hiddenReasoning")) {
            return new EvidenceCandidate(LearningResult.FAIL, List.of(), List.of("isolation"), "conflict");
        }
        String expected = String.valueOf(taskPackage.get("answerKey"));
        boolean pass = answer != null && (answer.contains(expected) || answer.contains("25"));
        return new EvidenceCandidate(
                pass ? LearningResult.PASS : LearningResult.FAIL,
                pass ? List.of("percent-change") : List.of(),
                pass ? List.of() : List.of("percent-change"),
                "deterministic-fake"
        );
    }
}
