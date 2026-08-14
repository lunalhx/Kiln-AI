package cn.lunalhx.ai.kilnai.domain.apply.store;

import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningResult;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InMemoryEvidenceStoreTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-08-14T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void acceptsEvidenceAtMostOncePerTaskAttempt() {
        InMemoryEvidenceStore store = new InMemoryEvidenceStore();
        UUID attemptId = UUID.randomUUID();
        AcceptedLearningEvidence evidence = new AcceptedLearningEvidence(
                UUID.randomUUID(), attemptId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                LearningResult.PASS, AttemptPurpose.INDEPENDENT_TEST, 0, List.of(), CLOCK.instant());

        store.accept(evidence);
        store.accept(evidence);

        assertEquals(1, store.allEvidence().size(),
                "a duplicate accept for the same task attempt must never create a second record");
        assertEquals(evidence, store.allEvidence().get(0));
    }
}
