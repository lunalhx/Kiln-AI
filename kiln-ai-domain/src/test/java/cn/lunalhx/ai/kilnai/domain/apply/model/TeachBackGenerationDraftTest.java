package cn.lunalhx.ai.kilnai.domain.apply.model;

import cn.lunalhx.ai.kilnai.domain.apply.fake.TeachBackScriptData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The strict parse contract of the closed {@code teach_back_generation/v1}
 * draft: a task-ready outcome with exactly the permitted fields, the
 * three-dimensional Rubric mapping, a grounded source trace, and an anchor
 * reference pinned to the supplied anchor — or a closed source-gap outcome.
 * Unknown fields, wrong schemas, and generic private maps are rejected.
 */
class TeachBackGenerationDraftTest {

    @Test
    void parsesAValidTaskReadyDraftWithTheThreeRubricDimensions() {
        TeachBackGenerationDraft draft = TeachBackGenerationDraft.parse(TeachBackScriptData.taskReadyJson());

        TeachBackGenerationDraft.TaskReady ready = assertInstanceOf(TeachBackGenerationDraft.TaskReady.class, draft);
        assertEquals(TeachBackGenerationDraft.SCHEMA, ready.schema());
        assertEquals("task_ready", ready.outcome());
        assertEquals(TeachBackScriptData.LEARNER_PROMPT, ready.learnerPrompt());
        assertEquals(List.of("rule_identification", "applicability_explanation", "steps_result_coherence"),
                ready.rubricMapping().stream().map(TeachBackGenerationDraft.RubricEntry::dimension).toList());
        assertEquals("differentiate-polynomial", ready.rubricMapping().get(0).masteryCriterion());
        assertEquals(1, ready.sourceTrace().size());
        assertEquals(TeachBackScriptData.SOURCE_PASSAGE, ready.sourceTrace().get(0).passageId());
        assertEquals(TeachBackScriptData.ANCHOR_ID.toString(), ready.anchorReference().anchorId());
        assertEquals(TeachBackScriptData.ANCHOR_KIND, ready.anchorReference().anchorKind());
    }

    @Test
    void parsesAClosedSourceGapOutcome() {
        TeachBackGenerationDraft draft = TeachBackGenerationDraft.parse(TeachBackScriptData.sourceGapJson());

        TeachBackGenerationDraft.SourceGap gap = assertInstanceOf(TeachBackGenerationDraft.SourceGap.class, draft);
        assertEquals("anchor_not_grounded", gap.sourceGap().reasonCode());
        assertEquals(List.of("rule_identification"), gap.sourceGap().missingRequirementIds());
    }

    @Test
    void rejectsAnUnknownSchemaAndAnUnknownOutcome() {
        assertThrows(ApplyDraftException.class, () -> TeachBackGenerationDraft.parse(
                TeachBackScriptData.taskReadyJson().replace("teach_back_generation/v1", "generic_teaching/v1")));
        assertThrows(ApplyDraftException.class, () -> TeachBackGenerationDraft.parse(
                TeachBackScriptData.taskReadyJson().replace("\"outcome\": \"task_ready\"", "\"outcome\": \"draft\"")));
    }

    @Test
    void rejectsUnknownFieldsAndGenericPrivateMaps() {
        String polluted = TeachBackScriptData.taskReadyJson().replace(
                "\"learner_prompt\"", "\"private_map\": {}, \"learner_prompt\"");
        assertThrows(ApplyDraftException.class, () -> TeachBackGenerationDraft.parse(polluted));
    }

    @Test
    void rejectsAWrongRubricMappingShape() {
        String missingMasteryCriterion = TeachBackScriptData.taskReadyJson().replace(
                "\"mastery_criterion\": \"differentiate-polynomial\"", "");
        assertThrows(ApplyDraftException.class, () -> TeachBackGenerationDraft.parse(missingMasteryCriterion));
    }

    @Test
    void rejectsAnInvalidAnchorReference() {
        String nonUuidAnchor = TeachBackScriptData.taskReadyJson().replace(
                TeachBackScriptData.ANCHOR_ID.toString(), "not-a-uuid");
        assertThrows(ApplyDraftException.class, () -> TeachBackGenerationDraft.parse(nonUuidAnchor));
        String missingKind = TeachBackScriptData.taskReadyJson().replace(
                "\"anchor_kind\": \"EXPLAIN_WORKED_EXAMPLE\"", "\"anchor_kind\": \"\"");
        assertThrows(ApplyDraftException.class, () -> TeachBackGenerationDraft.parse(missingKind));
    }

    @Test
    void rejectsAnEmptySourceTraceOrLearnerPrompt() {
        String emptyTrace = TeachBackScriptData.taskReadyJson().replace(
                "{ \"source_document_id\": \"openstax-calculus-v1\", \"passage_id\": \"sec-3.3-differentiation-rules\" }",
                "");
        assertThrows(ApplyDraftException.class, () -> TeachBackGenerationDraft.parse(emptyTrace));
        String blankPrompt = TeachBackScriptData.taskReadyJson("   ");
        assertThrows(ApplyDraftException.class, () -> TeachBackGenerationDraft.parse(blankPrompt));
    }
}
