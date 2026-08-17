package cn.lunalhx.ai.kilnai.domain.apply.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TeachBackAssessmentTest {

    private static final String VALID = """
            {"schema":"teach_back_assessment/v1",
             "rule_identification":"pass","applicability_explanation":"fail",
             "steps_result_coherence":"inconclusive","reason_codes":["rule_not_identified"]}
            """;

    @Test
    void parsesTheThreeDimensionContract() {
        TeachBackAssessment assessment = TeachBackAssessment.parse(VALID);

        assertEquals(TeachBackAssessment.DimensionJudgment.PASS, assessment.ruleIdentification());
        assertEquals(TeachBackAssessment.DimensionJudgment.FAIL, assessment.applicabilityExplanation());
        assertEquals(TeachBackAssessment.DimensionJudgment.INCONCLUSIVE, assessment.stepsResultCoherence());
        assertEquals(TeachBackAssessment.TeachBackOutcome.INCONCLUSIVE, assessment.outcome());
        assertEquals(List.of("rule_not_identified"), assessment.reasonCodes());
    }

    @Test
    void rejectsUnknownFields() {
        ModelContractInvalidException error = assertThrows(ModelContractInvalidException.class,
                () -> TeachBackAssessment.parse(VALID.replace(
                        "\"reason_codes\":", "\"hidden_reasoning\":\"no\",\"reason_codes\":")));
        assertEquals(List.of("unknown_field"), error.violationCodes());
        assertFalse(error.getMessage().contains("hidden_reasoning"));
    }

    @Test
    void rejectsInvalidDimensionEnum() {
        ModelContractInvalidException error = assertThrows(ModelContractInvalidException.class,
                () -> TeachBackAssessment.parse(VALID.replace(
                        "\"rule_identification\":\"pass\"", "\"rule_identification\":\"maybe\"")));
        assertEquals(List.of("invalid_enum"), error.violationCodes());
    }

    @Test
    void rejectsNullDimension() {
        ModelContractInvalidException error = assertThrows(ModelContractInvalidException.class,
                () -> TeachBackAssessment.parse("""
                        {"schema":"teach_back_assessment/v1",
                         "rule_identification":null,"applicability_explanation":"pass",
                         "steps_result_coherence":"pass","reason_codes":[]}
                        """));
        assertEquals(List.of("null_required"), error.violationCodes());
    }
}
