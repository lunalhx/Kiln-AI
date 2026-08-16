package cn.lunalhx.ai.kilnai;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;

import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.FinalExpressionJudgment;
import cn.lunalhx.ai.kilnai.domain.apply.model.RationaleJudgment;
import cn.lunalhx.ai.kilnai.domain.apply.model.ResponseAssessment;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskVerificationVerdict;
import cn.lunalhx.ai.kilnai.domain.apply.port.ApplyGenerationPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.AssessmentPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.OperatorModelProfilePort;
import cn.lunalhx.ai.kilnai.domain.apply.port.ResponseVerificationPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.TaskVerifierPort;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.Map;

/**
 * The scripted model ports for the Inconclusive Review Postgres contract: the
 * generation is deterministic by the Exposure Ledger count, Assessment always
 * judges the final expression equivalent, and Response Verification disagrees,
 * so a Review submission whose deterministic Mathematical Equivalence Check
 * cannot decide resolves to Inconclusive and triggers the replacement path.
 * The generation can be told to fail the next Review generation once, which
 * exercises the Source Gap resume path.
 */
@TestConfiguration
public class InconclusiveReviewPortsConfiguration {

    private volatile boolean failNextReviewGeneration = false;

    public void failNextReviewGeneration() {
        this.failNextReviewGeneration = true;
    }

    @Bean
    @Primary
    OperatorModelProfilePort scriptedOperatorModelProfile() {
        return ScriptedApplyPortsConfiguration::scriptedModelProfile;
    }

    @Bean
    @Primary
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
                int reviewNumber = exposedTaskCount(executionContextJson) - 2 + 1;
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

    private static int exposedTaskCount(String executionContextJson) {
        String marker = "\"exposed_task_fingerprints\":[";
        int start = executionContextJson.indexOf(marker);
        int end = executionContextJson.indexOf(']', start);
        String contents = executionContextJson.substring(start + marker.length(), end).trim();
        return contents.isEmpty() ? 0 : contents.split(",").length;
    }

    @Bean
    @Primary
    TaskVerifierPort scriptedApplyTaskVerifier() {
        return new TaskVerifierPort() {
            @Override
            public TaskVerificationVerdict verify(
                    ModelProfile profile,
                    TaskPackage taskPackage,
                    ApplyExecutionContext context
            ) {
                return new TaskVerificationVerdict(
                        TaskVerificationVerdict.SCHEMA,
                        TaskVerificationVerdict.Verdict.PASS,
                        Map.of(
                                "answer_correctness", TaskVerificationVerdict.CheckResult.PASS,
                                "rubric_alignment", TaskVerificationVerdict.CheckResult.PASS,
                                "source_grounding", TaskVerificationVerdict.CheckResult.PASS,
                                "blueprint_compliance", TaskVerificationVerdict.CheckResult.PASS,
                                "learner_boundary", TaskVerificationVerdict.CheckResult.PASS),
                        List.of());
            }
        };
    }

    @Bean
    @Primary
    AssessmentPort scriptedApplyAssessment() {
        return (profile, context) -> new ResponseAssessment(
                ResponseAssessment.SCHEMA,
                FinalExpressionJudgment.EQUIVALENT,
                RationaleJudgment.NOT_PROVIDED,
                List.of());
    }

    @Bean
    @Primary
    ResponseVerificationPort scriptedApplyResponseVerification() {
        return (profile, context) -> new ResponseAssessment(
                ResponseAssessment.SCHEMA,
                FinalExpressionJudgment.NOT_EQUIVALENT,
                RationaleJudgment.NOT_PROVIDED,
                List.of());
    }
}
