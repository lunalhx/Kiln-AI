package cn.lunalhx.ai.kilnai.domain.apply.model;

import cn.lunalhx.ai.kilnai.domain.apply.fake.ApplyScriptData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplyGenerationDraftTest {

    @Test
    void parsesAValidTaskReadyDraft() {
        ApplyGenerationDraft draft = ApplyGenerationDraft.parse(ApplyScriptData.taskReadyJson());

        assertTrue(draft instanceof ApplyGenerationDraft.TaskReady);
        ApplyGenerationDraft.TaskReady taskReady = (ApplyGenerationDraft.TaskReady) draft;
        assertEquals(ApplyGenerationDraft.SCHEMA, taskReady.schema());
        assertEquals("task_ready", taskReady.outcome());
        assertEquals(ApplyScriptData.TASK_TEXT, taskReady.learnerTaskText());
        assertEquals(ApplyScriptData.EXPECTED_EXPRESSION,
                taskReady.privateAssessorFacts().proposedExpectedAnswer().expression());
        assertEquals("differentiate-polynomial",
                taskReady.privateAssessorFacts().rubricMapping().get(0).masteryCriterionId());
        assertEquals("sec-3.3-differentiation-rules",
                taskReady.privateAssessorFacts().sourceTrace().get(0).passageId());
        assertEquals("real", taskReady.privateAssessorFacts().equivalenceDeclaration().domain());
    }

    @Test
    void parsesAValidSourceGapDraft() {
        ApplyGenerationDraft draft = ApplyGenerationDraft.parse(ApplyScriptData.sourceGapJson());

        assertTrue(draft instanceof ApplyGenerationDraft.SourceGap);
        ApplyGenerationDraft.SourceGap sourceGap = (ApplyGenerationDraft.SourceGap) draft;
        assertEquals("source_gap", sourceGap.outcome());
        assertEquals("required_criterion_not_grounded", sourceGap.sourceGap().reasonCode());
        assertEquals(java.util.List.of("differentiate-polynomial"), sourceGap.sourceGap().missingRequirementIds());
    }

    @Test
    void rejectsUnknownTopLevelFields() {
        assertThrows(ApplyDraftException.class, () -> ApplyGenerationDraft.parse(
                withField(ApplyScriptData.taskReadyJson(), "task_fingerprint", "\"fp-1\"")));
        assertThrows(ApplyDraftException.class, () -> ApplyGenerationDraft.parse(
                withField(ApplyScriptData.taskReadyJson(), "allowed_events", "[\"answer_submitted\"]")));
        assertThrows(ApplyDraftException.class, () -> ApplyGenerationDraft.parse(
                withField(ApplyScriptData.taskReadyJson(), "answer", "\"12*x^2\"")));
        assertThrows(ApplyDraftException.class, () -> ApplyGenerationDraft.parse(
                withField(ApplyScriptData.taskReadyJson(), "hidden_reasoning", "\"chain of thought\"")));
        assertThrows(ApplyDraftException.class, () -> ApplyGenerationDraft.parse(
                withField(ApplyScriptData.taskReadyJson(), "tool_calls", "[{\"name\":\"calculator\"}]")));
    }

    @Test
    void rejectsUnknownFieldsInsidePrivateAssessorFacts() {
        String json = ApplyScriptData.taskReadyJson();
        String polluted = json.replace(
                "\"equivalence_declaration\":",
                "\"canonical_answer\": \"12*x^2\", \"equivalence_declaration\":");
        assertThrows(ApplyDraftException.class, () -> ApplyGenerationDraft.parse(polluted));
    }

    @Test
    void rejectsWrongSchemaOrMissingOutcome() {
        assertThrows(ApplyDraftException.class, () -> ApplyGenerationDraft.parse(
                ApplyScriptData.taskReadyJson().replace("apply_generation/v1", "task_package/v1")));
        assertThrows(ApplyDraftException.class, () -> ApplyGenerationDraft.parse(
                ApplyScriptData.taskReadyJson().replace("\"outcome\": \"task_ready\",", "")));
    }

    @Test
    void rejectsUnknownOutcomeValues() {
        assertThrows(ApplyDraftException.class, () -> ApplyGenerationDraft.parse(
                ApplyScriptData.taskReadyJson().replace("\"outcome\": \"task_ready\"", "\"outcome\": \"practice\"")));
    }

    @Test
    void rejectsMalformedJsonAndTrailingContent() {
        assertThrows(ApplyDraftException.class, () -> ApplyGenerationDraft.parse("{not json"));
        assertThrows(ApplyDraftException.class, () -> ApplyGenerationDraft.parse(
                ApplyScriptData.taskReadyJson() + " trailing"));
        assertThrows(ApplyDraftException.class, () -> ApplyGenerationDraft.parse("null"));
    }

    @Test
    void rejectsBlankOrNonTextFields() {
        assertThrows(ApplyDraftException.class, () -> ApplyGenerationDraft.parse(
                ApplyScriptData.taskReadyJson().replace(
                        "\"learner_task_text\": \"" + ApplyScriptData.TASK_TEXT + "\"",
                        "\"learner_task_text\": \"  \"")));
    }

    private static String withField(String json, String field, String value) {
        return json.replace("\"outcome\": \"task_ready\"",
                "\"outcome\": \"task_ready\", \"" + field + "\": " + value);
    }
}
