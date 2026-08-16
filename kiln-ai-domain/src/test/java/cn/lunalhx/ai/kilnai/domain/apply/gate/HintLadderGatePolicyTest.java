package cn.lunalhx.ai.kilnai.domain.apply.gate;

import cn.lunalhx.ai.kilnai.domain.apply.fake.HintScriptData;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintGenerationDraft;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintLadder;
import cn.lunalhx.ai.kilnai.domain.gate.GateContext;
import cn.lunalhx.ai.kilnai.domain.gate.GateOutcome;
import cn.lunalhx.ai.kilnai.domain.gate.GateResult;
import cn.lunalhx.ai.kilnai.domain.gate.TypedArtifactGatePipeline;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Hint Ladder Gate: contract closure is enforced by the strict draft
 * parser, and this policy adds level order, disclosure-kind sequence, source
 * grounding, H1-H4 answer leakage, and deterministic polynomial H5 answer
 * equivalence. A rejected ladder must never reach the learner or state.
 */
class HintLadderGatePolicyTest {

    private static final UUID ATTEMPT_ID = UUID.randomUUID();

    private static final HintGateFacts FACTS = new HintGateFacts(
            HintScriptData.PRACTICE_EXPECTED,
            List.of("x"),
            List.of(new HintGateFacts.SourceRef(HintScriptData.SOURCE_DOCUMENT, HintScriptData.SOURCE_PASSAGE)));

    @Test
    void acceptsACompleteGroundedLadder() {
        GateResult<HintGenerationDraft.LadderReady> result = gate(HintScriptData.ladderReadyJson());
        assertEquals(GateOutcome.PASSED, result.outcome());
        HintLadder ladder = HintLadder.from(ATTEMPT_ID, result.artifact());
        assertEquals(ATTEMPT_ID, ladder.attemptId());
        assertEquals(5, ladder.entries().size());
        assertEquals("18*x^2-4", ladder.entry(5).proposedFinalAnswer());
    }

    @Test
    void rejectsLevelsOutOfOrder() {
        String json = HintScriptData.ladderReadyJson()
                .replaceFirst("\"level\": 1,\n\\s*\"disclosure_kind\": \"orient\"",
                        "\"level\": 2,\n                      \"disclosure_kind\": \"orient\"")
                .replaceFirst("\"level\": 2,\n\\s*\"disclosure_kind\": \"cue\"",
                        "\"level\": 1,\n                      \"disclosure_kind\": \"cue\"");
        GateResult<HintGenerationDraft.LadderReady> result = gate(json);
        assertEquals(GateOutcome.REJECTED, result.outcome());
        assertTrue(violation(result, "hint.level.order"));
    }

    @Test
    void rejectsADisclosureKindOutOfSequence() {
        String json = HintScriptData.ladderReadyJson()
                .replaceFirst("\"disclosure_kind\": \"orient\"", "\"disclosure_kind\": \"cue\"");
        GateResult<HintGenerationDraft.LadderReady> result = gate(json);
        assertEquals(GateOutcome.REJECTED, result.outcome());
        assertTrue(violation(result, "hint.kind.sequence"));
    }

    @Test
    void rejectsAnUngroundedSourceTrace() {
        String json = HintScriptData.ladderReadyJson().replace(
                "\"passage_id\": \"" + HintScriptData.SOURCE_PASSAGE + "\"",
                "\"passage_id\": \"sec-3.4-unapproved\"");
        GateResult<HintGenerationDraft.LadderReady> result = gate(json);
        assertEquals(GateOutcome.REJECTED, result.outcome());
        assertTrue(violation(result, "hint.source.ungrounded"));
    }

    @Test
    void rejectsAnswerLeakageInH1ToH4() {
        String json = HintScriptData.ladderReadyJson(
                "下一步的答案是 18x²−4。", HintScriptData.H5_LEARNER_CONTENT,
                HintScriptData.H5_STEPS, "18*x^2-4");
        GateResult<HintGenerationDraft.LadderReady> result = gate(json);
        assertEquals(GateOutcome.REJECTED, result.outcome());
        assertTrue(violation(result, "hint.answer.leak"));
    }

    @Test
    void rejectsTheCanonicalAnswerVerbatimInH4() {
        String json = HintScriptData.ladderReadyJson(
                "合并后得到 18*x^2-4。", HintScriptData.H5_LEARNER_CONTENT,
                HintScriptData.H5_STEPS, "18*x^2-4");
        GateResult<HintGenerationDraft.LadderReady> result = gate(json);
        assertEquals(GateOutcome.REJECTED, result.outcome());
        assertTrue(violation(result, "hint.answer.leak"));
    }

    @Test
    void rejectsANonEquivalentProposedFinalAnswer() {
        String json = HintScriptData.ladderReadyJson(
                HintScriptData.H4_SCAFFOLD, HintScriptData.H5_LEARNER_CONTENT,
                HintScriptData.H5_STEPS, "6*x^2-4");
        GateResult<HintGenerationDraft.LadderReady> result = gate(json);
        assertEquals(GateOutcome.REJECTED, result.outcome());
        assertTrue(violation(result, "hint.h5.equivalence"));
    }

    @Test
    void acceptsAnEquivalentButDifferentlyWrittenFinalAnswer() {
        String json = HintScriptData.ladderReadyJson(
                HintScriptData.H4_SCAFFOLD, HintScriptData.H5_LEARNER_CONTENT,
                HintScriptData.H5_STEPS, "-4+18*x^2");
        GateResult<HintGenerationDraft.LadderReady> result = gate(json);
        assertEquals(GateOutcome.PASSED, result.outcome());
    }

    @Test
    void rejectsAnAnswerTheCheckerCannotDecide() {
        String json = HintScriptData.ladderReadyJson(
                HintScriptData.H4_SCAFFOLD, HintScriptData.H5_LEARNER_CONTENT,
                HintScriptData.H5_STEPS, "18x²−4");
        GateResult<HintGenerationDraft.LadderReady> result = gate(json);
        assertEquals(GateOutcome.REJECTED, result.outcome());
        assertTrue(violation(result, "hint.h5.equivalence"),
                "an unparseable proposed answer must be rejected, never guessed");
    }

    @Test
    void rejectsALeakHiddenInAReasonableLookingStep() {
        String json = HintScriptData.ladderReadyJson(
                "合并后是 18*x^2 - 4，检查各项系数。", HintScriptData.H5_LEARNER_CONTENT,
                HintScriptData.H5_STEPS, "18*x^2-4");
        GateResult<HintGenerationDraft.LadderReady> result = gate(json);
        assertEquals(GateOutcome.REJECTED, result.outcome());
        assertFalse(result.violations().isEmpty());
    }

    private static GateResult<HintGenerationDraft.LadderReady> gate(String json) {
        HintGenerationDraft draft = HintGenerationDraft.parse(json);
        HintGenerationDraft.LadderReady ladder = (HintGenerationDraft.LadderReady) draft;
        return new TypedArtifactGatePipeline().validate(
                ladder, new HintLadderGatePolicy(FACTS), GateContext.empty());
    }

    private static boolean violation(GateResult<HintGenerationDraft.LadderReady> result, String code) {
        return result.violations().stream().anyMatch(v -> v.code().equals(code));
    }
}
