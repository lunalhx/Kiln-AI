package cn.lunalhx.ai.kilnai.domain.apply.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponseAssessmentTest {

    private static final String VALID = """
            {"schema":"response_assessment/v1",
             "final_expression_judgment":"equivalent",
             "rationale_judgment":"not_provided",
             "reason_codes":[]}
            """;

    @Test
    void parsesAClosedValidContract() {
        ResponseAssessment assessment = ResponseAssessment.parse(VALID);

        assertEquals(ResponseAssessment.SCHEMA, assessment.schema());
        assertEquals(FinalExpressionJudgment.EQUIVALENT, assessment.finalExpressionJudgment());
        assertEquals(RationaleJudgment.NOT_PROVIDED, assessment.rationaleJudgment());
        assertEquals(List.of(), assessment.reasonCodes());
    }

    @Test
    void rejectsUnknownFields() {
        ModelContractInvalidException error = assertThrows(ModelContractInvalidException.class,
                () -> ResponseAssessment.parse(withField(VALID, "hidden_reasoning", "\"chain of thought\"")));
        assertEquals(List.of("unknown_field"), error.violationCodes());
        assertFalse(error.getMessage().contains("chain of thought"));
        assertFalse(error.getMessage().contains("hidden_reasoning"));
    }

    @Test
    void rejectsMissingRequiredFields() {
        ModelContractInvalidException error = assertThrows(ModelContractInvalidException.class,
                () -> ResponseAssessment.parse("""
                        {"schema":"response_assessment/v1",
                         "final_expression_judgment":"equivalent",
                         "reason_codes":[]}
                        """));
        assertEquals(List.of("missing_field"), error.violationCodes());
    }

    @Test
    void rejectsNullRequiredValues() {
        ModelContractInvalidException error = assertThrows(ModelContractInvalidException.class,
                () -> ResponseAssessment.parse("""
                        {"schema":"response_assessment/v1",
                         "final_expression_judgment":null,
                         "rationale_judgment":"not_provided",
                         "reason_codes":[]}
                        """));
        assertEquals(List.of("null_required"), error.violationCodes());
    }

    @Test
    void rejectsInvalidEnums() {
        ModelContractInvalidException error = assertThrows(ModelContractInvalidException.class,
                () -> ResponseAssessment.parse("""
                        {"schema":"response_assessment/v1",
                         "final_expression_judgment":"pass",
                         "rationale_judgment":"not_provided",
                         "reason_codes":[]}
                        """));
        assertEquals(List.of("invalid_enum"), error.violationCodes());
    }

    @Test
    void rejectsWrongSchema() {
        ModelContractInvalidException error = assertThrows(ModelContractInvalidException.class,
                () -> ResponseAssessment.parse(VALID.replace("response_assessment/v1", "response_assessment/v2")));
        assertEquals(List.of("wrong_schema"), error.violationCodes());
    }

    @Test
    void rejectsInvalidCollectionShape() {
        ModelContractInvalidException error = assertThrows(ModelContractInvalidException.class,
                () -> ResponseAssessment.parse("""
                        {"schema":"response_assessment/v1",
                         "final_expression_judgment":"equivalent",
                         "rationale_judgment":"not_provided",
                         "reason_codes":"not-an-array"}
                        """));
        assertEquals(List.of("invalid_collection"), error.violationCodes());
    }

    @Test
    void rejectsInvalidJsonWithoutExposingParserDetails() {
        ModelContractInvalidException error = assertThrows(ModelContractInvalidException.class,
                () -> ResponseAssessment.parse("{not json"));
        assertEquals(List.of("invalid_json"), error.violationCodes());
        assertFalse(error.getMessage().contains("JsonParseException"));
        assertFalse(error.getMessage().contains("{not json"));
    }

    @Test
    void violationCodesAreNormalizedAndDoNotRetainRawOutput() {
        ModelContractInvalidException error = assertThrows(ModelContractInvalidException.class,
                () -> ResponseAssessment.parse("""
                        {"schema":"response_assessment/v1",
                         "final_expression_judgment":"equivalent",
                         "rationale_judgment":"not_provided",
                         "reason_codes":[],
                         "learner_answer":"12*x^2"}
                        """));
        assertEquals(List.of("unknown_field"), error.violationCodes());
        assertTrue(error.violationCodes().stream().allMatch(code -> code.matches("[a-z_]+")));
        assertFalse(error.getMessage().contains("12*x^2"));
        assertFalse(error.getMessage().contains("learner_answer"));
    }

    private static String withField(String json, String field, String value) {
        return json.replace("\"reason_codes\":[]", "\"" + field + "\":" + value + ",\"reason_codes\":[]");
    }
}
