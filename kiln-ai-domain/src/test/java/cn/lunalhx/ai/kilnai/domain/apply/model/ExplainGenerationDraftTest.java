package cn.lunalhx.ai.kilnai.domain.apply.model;

import cn.lunalhx.ai.kilnai.domain.apply.fake.ExplainScriptData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExplainGenerationDraftTest {

    @Test
    void parsesAValidTeachingReadyDraft() {
        ExplainGenerationDraft.TeachingReady draft = assertInstanceOf(
                ExplainGenerationDraft.TeachingReady.class, ExplainGenerationDraft.parse(ExplainScriptData.explainReadyJson()));

        assertEquals("explain_generation/v1", draft.schema());
        assertEquals("teaching_ready", draft.outcome());
        assertEquals(ExplainScriptData.PRINCIPLE_SUMMARY, draft.principleSummary());
        assertEquals(ExplainScriptData.EXPLAIN_PROBLEM, draft.workedExample().problem());
        assertEquals(4, draft.workedExample().steps().size());
        assertEquals("constant-multiple rule", draft.workedExample().steps().get(0).ruleReference());
        assertEquals("幂法则：指数 3 乘入系数，指数降为 2。", draft.workedExample().steps().get(1).explanation());
        assertEquals(ExplainScriptData.EXPLAIN_FINAL_RESULT, draft.workedExample().finalResult());
        assertEquals(1, draft.sourceTrace().size());
        assertEquals("openstax-calculus-v1", draft.sourceTrace().get(0).sourceDocumentId());
        assertEquals("sec-3.3-differentiation-rules", draft.sourceTrace().get(0).passageId());
    }

    @Test
    void parsesASourceGapDraft() {
        ExplainGenerationDraft.SourceGap gap = assertInstanceOf(
                ExplainGenerationDraft.SourceGap.class, ExplainGenerationDraft.parse(ExplainScriptData.explainSourceGapJson()));

        assertEquals("explain_generation/v1", gap.schema());
        assertEquals("source_gap", gap.outcome());
        assertEquals("required_rule_not_grounded", gap.sourceGap().reasonCode());
        assertEquals(java.util.List.of("sum and difference rules"), gap.sourceGap().missingRequirementIds());
    }

    @Test
    void rejectsAnUnsupportedSchema() {
        String polluted = ExplainScriptData.explainReadyJson().replace("explain_generation/v1", "apply_generation/v1");
        assertThrows(ApplyDraftException.class, () -> ExplainGenerationDraft.parse(polluted));
    }

    @Test
    void rejectsAnUnknownOutcome() {
        String polluted = ExplainScriptData.explainReadyJson().replace("\"teaching_ready\"", "\"task_ready\"");
        assertThrows(ApplyDraftException.class, () -> ExplainGenerationDraft.parse(polluted));
    }

    @Test
    void rejectsUnknownFields() {
        String polluted = ExplainScriptData.explainReadyJson().replace(
                "\"principle_summary\"", "\"pedagogy_plan\": \"x\", \"principle_summary\"");
        assertThrows(ApplyDraftException.class, () -> ExplainGenerationDraft.parse(polluted));
    }

    @Test
    void rejectsASecondWorkedExampleField() {
        String polluted = ExplainScriptData.explainReadyJson().replace(
                "\"source_trace\"", "\"worked_example_2\": {}, \"source_trace\"");
        assertThrows(ApplyDraftException.class, () -> ExplainGenerationDraft.parse(polluted));
    }

    @Test
    void rejectsAMissingStepField() {
        String polluted = ExplainScriptData.explainReadyJson().replace(
                "\"explanation\"", "\"extra\": true, \"explanation\"");
        assertThrows(ApplyDraftException.class, () -> ExplainGenerationDraft.parse(polluted));
    }

    @Test
    void rejectsNonJsonInput() {
        assertThrows(ApplyDraftException.class, () -> ExplainGenerationDraft.parse("{not valid json"));
    }
}
