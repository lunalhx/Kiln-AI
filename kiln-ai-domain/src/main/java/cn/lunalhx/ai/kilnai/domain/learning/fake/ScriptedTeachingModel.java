package cn.lunalhx.ai.kilnai.domain.learning.fake;

import cn.lunalhx.ai.kilnai.domain.artifact.TeachingResultEnvelope;
import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.TeachingModelPort;
import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.ToolSession;
import cn.lunalhx.ai.kilnai.domain.learning.model.TeachingContextView;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearnerInputKind;
import cn.lunalhx.ai.kilnai.domain.pedagogy.model.valobj.TeachingAction;
import cn.lunalhx.ai.kilnai.domain.skill.SkillStack;
import cn.lunalhx.ai.kilnai.domain.tool.ToolHandle;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ScriptedTeachingModel implements TeachingModelPort {

    private final ScriptedScenario scenario;
    private boolean repaired;

    public ScriptedTeachingModel(ScriptedScenario scenario) {
        this.scenario = scenario;
    }

    @Override
    public TeachingResultEnvelope teach(
            TeachingAction action,
            TeachingContextView context,
            SkillStack stack,
            String compiledPrompt,
            List<ToolHandle> tools,
            ToolSession toolSession
    ) {
        if (action == TeachingAction.EXPLAIN) {
            return explain();
        }
        if (scenario == ScriptedScenario.REPAIRABLE_ONCE && !repaired) {
            repaired = true;
            return apply(false, toolSession);
        }
        if (scenario == ScriptedScenario.REJECTED) {
            return new TeachingResultEnvelope(
                    TeachingAction.APPLY, "answer is 25", Map.of(), List.of("source-1"),
                    List.of(LearnerInputKind.ANSWER_SUBMITTED), "hidden"
            );
        }
        return apply(true, toolSession);
    }

    private TeachingResultEnvelope explain() {
        return new TeachingResultEnvelope(
                TeachingAction.EXPLAIN,
                "Percent change is (new - old) / old × 100. For an increase, the value is positive.",
                Map.of("hiddenReasoning", "do not expose"),
                List.of("source-pack-percent-change"),
                List.of(LearnerInputKind.CONTINUE_REQUESTED, LearnerInputKind.CLARIFICATION_ASKED),
                "internal-explain-trace"
        );
    }

    private TeachingResultEnvelope apply(boolean complete, ToolSession toolSession) {
        Object answer = toolSession.call("calculator@1", Map.of("old", 80, "new", 100)).get("result");
        Map<String, Object> privateArtifacts = new LinkedHashMap<>();
        if (complete) {
            privateArtifacts.put("answerKey", String.valueOf(answer));
            privateArtifacts.put("taskRubric", "correct percent change from 80 to 100");
        }
        return new TeachingResultEnvelope(
                TeachingAction.APPLY,
                "A quantity grows from 80 to 100. What is the percent change?",
                privateArtifacts,
                List.of("source-pack-percent-change"),
                List.of(LearnerInputKind.ANSWER_SUBMITTED, LearnerInputKind.HINT_REQUESTED),
                "internal-apply-trace"
        );
    }
}
