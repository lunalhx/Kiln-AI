package cn.lunalhx.ai.kilnai.domain.apply.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskVerificationVerdictTest {

    private static final String VALID = """
            {"schema":"task_verification/v1","verdict":"pass",
             "checks":{"answer_correctness":"pass","rubric_alignment":"pass","source_grounding":"pass",
             "blueprint_compliance":"pass","learner_boundary":"pass"},"reason_codes":[]}
            """;

    @Test
    void parsesAClosedPassVerdict() {
        TaskVerificationVerdict verdict = TaskVerificationVerdict.parse(VALID);

        assertEquals(TaskVerificationVerdict.SCHEMA, verdict.schema());
        assertEquals(TaskVerificationVerdict.Verdict.PASS, verdict.verdict());
        assertTrue(verdict.passed());
        assertEquals(5, verdict.checks().size());
        assertTrue(verdict.checks().values().stream()
                .allMatch(result -> result == TaskVerificationVerdict.CheckResult.PASS));
    }

    @Test
    void rejectsUnknownFields() {
        ModelContractInvalidException error = assertThrows(ModelContractInvalidException.class,
                () -> TaskVerificationVerdict.parse(VALID.replace(
                        "\"reason_codes\":[]", "\"hidden_reasoning\":\"no\",\"reason_codes\":[]")));
        assertEquals(List.of("unknown_field"), error.violationCodes());
        assertFalse(error.getMessage().contains("hidden_reasoning"));
    }

    @Test
    void rejectsUnknownCheckFields() {
        ModelContractInvalidException error = assertThrows(ModelContractInvalidException.class,
                () -> TaskVerificationVerdict.parse(VALID.replace(
                        "\"learner_boundary\":\"pass\"",
                        "\"learner_boundary\":\"pass\",\"hidden\":\"pass\"")));
        assertEquals(List.of("unknown_field"), error.violationCodes());
        assertFalse(error.getMessage().contains("hidden"));
    }

    @Test
    void rejectsMissingChecks() {
        ModelContractInvalidException error = assertThrows(ModelContractInvalidException.class,
                () -> TaskVerificationVerdict.parse("""
                        {"schema":"task_verification/v1","verdict":"pass","reason_codes":[]}
                        """));
        assertEquals(List.of("missing_field"), error.violationCodes());
    }

    @Test
    void rejectsNullVerdict() {
        ModelContractInvalidException error = assertThrows(ModelContractInvalidException.class,
                () -> TaskVerificationVerdict.parse("""
                        {"schema":"task_verification/v1","verdict":null,
                         "checks":{"answer_correctness":"pass"},"reason_codes":[]}
                        """));
        assertEquals(List.of("null_required"), error.violationCodes());
    }

    @Test
    void rejectsInvalidEnum() {
        ModelContractInvalidException error = assertThrows(ModelContractInvalidException.class,
                () -> TaskVerificationVerdict.parse(VALID.replace("\"verdict\":\"pass\"", "\"verdict\":\"ok\"")));
        assertEquals(List.of("invalid_enum"), error.violationCodes());
    }

    @Test
    void rejectsNonObjectChecks() {
        ModelContractInvalidException error = assertThrows(ModelContractInvalidException.class,
                () -> TaskVerificationVerdict.parse("""
                        {"schema":"task_verification/v1","verdict":"pass",
                         "checks":["pass"],"reason_codes":[]}
                        """));
        assertEquals(List.of("invalid_collection"), error.violationCodes());
    }

    @Test
    void rejectsWrongSchema() {
        ModelContractInvalidException error = assertThrows(ModelContractInvalidException.class,
                () -> TaskVerificationVerdict.parse(VALID.replace("task_verification/v1", "task_verification/v2")));
        assertEquals(List.of("wrong_schema"), error.violationCodes());
    }
}
