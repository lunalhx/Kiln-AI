package cn.lunalhx.ai.kilnai;

import cn.lunalhx.ai.kilnai.domain.apply.model.FinalExpressionJudgment;
import cn.lunalhx.ai.kilnai.domain.apply.model.RationaleJudgment;
import cn.lunalhx.ai.kilnai.domain.apply.model.ResponseAssessment;
import cn.lunalhx.ai.kilnai.domain.apply.port.ApplyGenerationPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.AssessmentPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.ResponseVerificationPort;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.List;

/**
 * The scripted graph ports of the Inconclusive Review contract over the
 * unified Learning Flow API: Assessment always judges the final expression
 * equivalent and Response Verification disagrees, so a Review submission
 * whose deterministic Mathematical Equivalence Check cannot decide resolves
 * to Inconclusive and triggers the replacement path. The generation can be
 * told to fail the next Review generation once, which exercises the Source
 * Gap resume path. All other graph ports are inherited from
 * {@link ScriptedLearningGraphPortsConfiguration}.
 */
@TestConfiguration
public class InconclusiveReviewGraphPortsConfiguration extends ScriptedLearningGraphPortsConfiguration {

    private volatile boolean failNextReviewGeneration = false;

    public void failNextReviewGeneration() {
        this.failNextReviewGeneration = true;
    }

    @Bean
    @Primary
    @Override
    ApplyGenerationPort scriptedApplyGeneration() {
        return (profile, compiledSystemPrompt, executionContextJson) -> {
            if (executionContextJson.contains("\"attempt_purpose\":\"review\"")) {
                if (failNextReviewGeneration) {
                    failNextReviewGeneration = false;
                    return """
                            {
                              "schema": "apply_generation/v1",
                              "outcome": "source_gap",
                              "source_gap": {
                                "reason_code": "required_criterion_not_grounded",
                                "missing_requirement_ids": ["differentiate-polynomial"]
                              }
                            }
                            """;
                }
                int reviewNumber = ScriptedApplyPortsConfiguration.exposedTaskCount(executionContextJson) - 2 + 1;
                return switch (reviewNumber) {
                    case 1 -> ScriptedApplyPortsConfiguration.taskReadyJson(
                            ScriptedApplyPortsConfiguration.REVIEW_TASK,
                            ScriptedApplyPortsConfiguration.REVIEW_EXPECTED);
                    default -> ScriptedApplyPortsConfiguration.taskReadyJson(
                            ScriptedApplyPortsConfiguration.REVIEW_TASK_2,
                            ScriptedApplyPortsConfiguration.REVIEW_EXPECTED_2);
                };
            }
            return executionContextJson.contains("\"attempt_purpose\":\"independent_test\"")
                    ? ScriptedApplyPortsConfiguration.taskReadyJson(
                            ScriptedApplyPortsConfiguration.INDEPENDENT_TASK,
                            ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED)
                    : ScriptedApplyPortsConfiguration.taskReadyJson(
                            ScriptedApplyPortsConfiguration.DIAGNOSTIC_TASK,
                            ScriptedApplyPortsConfiguration.DIAGNOSTIC_EXPECTED);
        };
    }

    @Bean
    @Primary
    @Override
    AssessmentPort scriptedApplyAssessment() {
        return (profile, context) -> new ResponseAssessment(
                ResponseAssessment.SCHEMA,
                FinalExpressionJudgment.EQUIVALENT,
                RationaleJudgment.NOT_PROVIDED,
                List.of());
    }

    @Bean
    @Primary
    @Override
    ResponseVerificationPort scriptedApplyResponseVerification() {
        return (profile, context) -> new ResponseAssessment(
                ResponseAssessment.SCHEMA,
                FinalExpressionJudgment.NOT_EQUIVALENT,
                RationaleJudgment.NOT_PROVIDED,
                List.of());
    }
}
