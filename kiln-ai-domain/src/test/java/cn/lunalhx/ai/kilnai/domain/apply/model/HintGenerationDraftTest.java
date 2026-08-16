package cn.lunalhx.ai.kilnai.domain.apply.model;

import cn.lunalhx.ai.kilnai.domain.apply.fake.HintScriptData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The closed {@code hint_generation/v1} contract: exactly five ordered
 * entries with per-level field sets, or a closed source-gap outcome. Unknown
 * fields, a wrong schema, an entry carrying H5-only fields, and malformed
 * payloads are rejected at parse time so the Gate never sees an open draft.
 */
class HintGenerationDraftTest {

    @Test
    void parsesAValidLadderReadyDraft() {
        HintGenerationDraft draft = HintGenerationDraft.parse(HintScriptData.ladderReadyJson());
        assertInstanceOf(HintGenerationDraft.LadderReady.class, draft);
        HintGenerationDraft.LadderReady ladder = (HintGenerationDraft.LadderReady) draft;
        assertEquals(HintGenerationDraft.SCHEMA, ladder.schema());
        assertEquals("ladder_ready", ladder.outcome());
        assertEquals(5, ladder.entries().size());
        for (int index = 0; index < 5; index++) {
            HintGenerationDraft.Entry entry = ladder.entries().get(index);
            assertEquals(index + 1, entry.level());
            assertEquals(HintLevel.of(index + 1).disclosureKind(), entry.disclosureKind());
            assertTrue(entry.sourceTrace().size() == 1);
            assertEquals(HintScriptData.SOURCE_PASSAGE, entry.sourceTrace().get(0).passageId());
        }
        HintGenerationDraft.Entry h5 = ladder.entries().get(4);
        assertEquals(4, h5.reasoningSteps().size());
        assertEquals("18*x^2-4", h5.proposedFinalAnswer());
        HintGenerationDraft.Entry h1 = ladder.entries().get(0);
        assertNull(h1.reasoningSteps(), "H1-H4 must never carry H5-only reasoning steps");
        assertNull(h1.proposedFinalAnswer(), "H1-H4 must never carry the final answer");
    }

    @Test
    void parsesAValidSourceGapDraft() {
        HintGenerationDraft draft = HintGenerationDraft.parse(HintScriptData.sourceGapJson());
        assertInstanceOf(HintGenerationDraft.SourceGap.class, draft);
        HintGenerationDraft.SourceGap gap = (HintGenerationDraft.SourceGap) draft;
        assertEquals("ladder_not_grounded", gap.sourceGap().reasonCode());
        assertEquals(java.util.List.of("h5-reveal"), gap.sourceGap().missingRequirementIds());
    }

    @Test
    void rejectsUnknownTopLevelFields() {
        assertThrows(ApplyDraftException.class, () -> HintGenerationDraft.parse(
                replace("\"outcome\": \"ladder_ready\",", "\"outcome\": \"ladder_ready\", \"next_level\": 1,")));
        assertThrows(ApplyDraftException.class, () -> HintGenerationDraft.parse(
                replace("\"outcome\": \"ladder_ready\",", "\"outcome\": \"ladder_ready\", \"exposed_levels\": [1],")));
    }

    @Test
    void rejectsUnknownFieldsInsideAnEntry() {
        String polluted = replace("\"learner_content\": \"" + HintScriptData.H1_ORIENT + "\"",
                "\"hidden_reasoning\": \"chain of thought\", \"learner_content\": \""
                        + HintScriptData.H1_ORIENT + "\"");
        assertThrows(ApplyDraftException.class, () -> HintGenerationDraft.parse(polluted));
    }

    @Test
    void rejectsH5OnlyFieldsOnLowerLevels() {
        String polluted = replace("\"learner_content\": \"" + HintScriptData.H4_SCAFFOLD + "\"",
                "\"learner_content\": \"" + HintScriptData.H4_SCAFFOLD
                        + "\", \"proposed_final_answer\": \"18*x^2-4\"");
        assertThrows(ApplyDraftException.class, () -> HintGenerationDraft.parse(polluted));
    }

    @Test
    void rejectsAMissingFinalAnswerOnH5() {
        String polluted = replace("\"proposed_final_answer\": \"18*x^2-4\"", "\"proposed_final_answer\": \" \"");
        assertThrows(ApplyDraftException.class, () -> HintGenerationDraft.parse(polluted));
    }

    @Test
    void rejectsMissingLearnerContent() {
        String polluted = replace("\"learner_content\": \"" + HintScriptData.H1_ORIENT + "\"",
                "\"learner_content\": \"  \"");
        assertThrows(ApplyDraftException.class, () -> HintGenerationDraft.parse(polluted));
    }

    @Test
    void rejectsAnEmptySourceTrace() {
        String polluted = HintScriptData.ladderReadyJson().replaceFirst(
                "\"source_trace\": \\[\\s*\\{ \"source_document_id\": \"" + HintScriptData.SOURCE_DOCUMENT
                        + "\", \"passage_id\": \"" + HintScriptData.SOURCE_PASSAGE + "\" \\}\\s*\\]",
                "\"source_trace\": []");
        assertThrows(ApplyDraftException.class, () -> HintGenerationDraft.parse(polluted));
    }

    @Test
    void rejectsAnyEntryCountOtherThanFive() {
        String oneEntry = ladderWithOnlyFirstEntry();
        assertThrows(ApplyDraftException.class, () -> HintGenerationDraft.parse(oneEntry));
    }

    @Test
    void rejectsWrongSchemaOrUnknownOutcome() {
        assertThrows(ApplyDraftException.class, () -> HintGenerationDraft.parse(
                replace("hint_generation/v1", "hint_generation/v2")));
        assertThrows(ApplyDraftException.class, () -> HintGenerationDraft.parse(
                replace("\"outcome\": \"ladder_ready\"", "\"outcome\": \"hint\"")));
        assertThrows(ApplyDraftException.class, () -> HintGenerationDraft.parse(
                HintScriptData.ladderReadyJson().replace("\"outcome\": \"ladder_ready\",", "")));
    }

    @Test
    void rejectsMalformedJsonTrailingContentAndNonObjectRoot() {
        assertThrows(ApplyDraftException.class, () -> HintGenerationDraft.parse("{not json"));
        assertThrows(ApplyDraftException.class, () -> HintGenerationDraft.parse(
                HintScriptData.ladderReadyJson() + " trailing"));
        assertThrows(ApplyDraftException.class, () -> HintGenerationDraft.parse("null"));
    }

    private static String replace(String target, String replacement) {
        return HintScriptData.ladderReadyJson().replace(target, replacement);
    }

    private static String ladderWithOnlyFirstEntry() {
        String full = HintScriptData.ladderReadyJson();
        int firstEnd = full.indexOf("\"level\": 2,");
        return full.substring(0, full.indexOf("\"entries\": [") + "\"entries\": [".length())
                + full.substring(full.indexOf("\"level\": 1,"), firstEnd)
                + "\n                ]\n}";
    }
}
