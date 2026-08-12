package cn.lunalhx.ai.kilnai.domain.learning.model.entity;

import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.ConceptState;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningEventType;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LearnerConceptProgressTest {

    private static final Instant NOW = Instant.parse("2026-08-13T00:00:00Z");

    @Test
    void assistedSuccessDoesNotCreateIndependentMastery() {
        LearnerConceptProgress progress = LearnerConceptProgress.start(UUID.randomUUID(), UUID.randomUUID(), NOW);

        ConceptState state = progress.record(new LearningEvidence(
                LearningEventType.APPLY, LearningResult.PASS, 2, false, false, NOW
        ));

        assertEquals(ConceptState.ASSISTED, state);
    }

    @Test
    void delayedAndTransferIndependentSuccessCreateDurableMastery() {
        LearnerConceptProgress progress = LearnerConceptProgress.start(UUID.randomUUID(), UUID.randomUUID(), NOW);
        progress.record(new LearningEvidence(
                LearningEventType.INDEPENDENT_TEST, LearningResult.PASS, 0, false, false, NOW
        ));
        progress.record(new LearningEvidence(
                LearningEventType.RETRIEVE, LearningResult.PASS, 0, true, false, NOW.plusSeconds(86_400)
        ));

        ConceptState state = progress.record(new LearningEvidence(
                LearningEventType.APPLY, LearningResult.PASS, 0, false, true, NOW.plusSeconds(172_800)
        ));

        assertEquals(ConceptState.DURABLE, state);
    }

    @Test
    void failureInvalidatesPriorIndependentEvidence() {
        LearnerConceptProgress progress = LearnerConceptProgress.start(UUID.randomUUID(), UUID.randomUUID(), NOW);
        progress.record(new LearningEvidence(
                LearningEventType.INDEPENDENT_TEST, LearningResult.PASS, 0, false, false, NOW
        ));
        progress.record(new LearningEvidence(
                LearningEventType.INDEPENDENT_TEST, LearningResult.FAIL, 0, false, false, NOW.plusSeconds(60)
        ));

        ConceptState state = progress.record(new LearningEvidence(
                LearningEventType.APPLY, LearningResult.PASS, 2, false, false, NOW.plusSeconds(120)
        ));

        assertEquals(ConceptState.ASSISTED, state);
    }
}
