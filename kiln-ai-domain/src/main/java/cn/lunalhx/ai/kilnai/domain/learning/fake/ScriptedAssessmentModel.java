package cn.lunalhx.ai.kilnai.domain.learning.fake;

import cn.lunalhx.ai.kilnai.domain.artifact.EvidenceCandidate;
import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.AssessmentModelPort;
import cn.lunalhx.ai.kilnai.domain.learning.model.AssessmentContextView;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningResult;

import java.util.List;
import java.util.Map;

public final class ScriptedAssessmentModel implements AssessmentModelPort {

    @Override
    public EvidenceCandidate assess(AssessmentContextView context, String compiledPrompt) {
        Map<String, Object> taskPackage = context.taskPackage();
        if (taskPackage.containsKey("hiddenReasoning")) {
            return new EvidenceCandidate(LearningResult.FAIL, List.of(), List.of("isolation"), "conflict");
        }
        String expected = String.valueOf(taskPackage.get("answerKey"));
        String answer = context.answer();
        boolean pass = answer != null && (answer.contains(expected) || answer.contains("25"));
        return new EvidenceCandidate(
                pass ? LearningResult.PASS : LearningResult.FAIL,
                pass ? List.of("percent-change") : List.of(),
                pass ? List.of() : List.of("percent-change"),
                "deterministic-fake"
        );
    }
}
