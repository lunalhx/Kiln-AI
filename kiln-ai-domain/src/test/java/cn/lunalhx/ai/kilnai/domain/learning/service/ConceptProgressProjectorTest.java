package cn.lunalhx.ai.kilnai.domain.learning.service;

import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ConceptProgress;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningResult;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.MasteryMilestone;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConceptProgressProjectorTest {

    private static final UUID LEARNER = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CONCEPT = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final Instant NOW = Instant.parse("2026-08-13T00:00:00Z");

    private final ConceptProgressProjector projector = new ConceptProgressProjector();

    @Test
    void noEvidenceProjectsUnassessed() {
        ConceptProgress progress = projector.project(LEARNER, CONCEPT, List.of());

        assertEquals(MasteryMilestone.UNASSESSED, progress.currentMilestone());
        assertEquals(MasteryMilestone.UNASSESSED, progress.highestMilestoneReached());
    }

    @Test
    void practiceEvidenceProjectsAtMostLearning() {
        ConceptProgress progress = projector.project(LEARNER, CONCEPT, List.of(
                evidence(AttemptPurpose.PRACTICE, LearningResult.PASS, 0)
        ));

        assertEquals(MasteryMilestone.LEARNING, progress.currentMilestone());
        assertEquals(MasteryMilestone.LEARNING, progress.highestMilestoneReached());
    }

    @Test
    void independentPassCanReachIndependentMilestone() {
        ConceptProgress progress = projector.project(LEARNER, CONCEPT, List.of(
                evidence(AttemptPurpose.INDEPENDENT_TEST, LearningResult.PASS, 0)
        ));

        assertEquals(MasteryMilestone.INDEPENDENT, progress.currentMilestone());
        assertEquals(MasteryMilestone.INDEPENDENT, progress.highestMilestoneReached());
    }

    @Test
    void highestMilestoneNeverDecreasesWhenCurrentFalls() {
        ConceptProgress progress = projector.project(LEARNER, CONCEPT, List.of(
                evidence(AttemptPurpose.INDEPENDENT_TEST, LearningResult.PASS, 0, NOW),
                evidence(AttemptPurpose.INDEPENDENT_TEST, LearningResult.FAIL, 0, NOW.plusSeconds(60))
        ));

        assertEquals(MasteryMilestone.LEARNING, progress.currentMilestone());
        assertEquals(MasteryMilestone.INDEPENDENT, progress.highestMilestoneReached());
    }

    @Test
    void theFoldOrderIsDeterministicByAcceptedAtThenEvidenceId() {
        Instant sameInstant = Instant.parse("2026-08-13T00:00:00Z");
        AcceptedLearningEvidence pass = evidence(AttemptPurpose.INDEPENDENT_TEST, LearningResult.PASS, 0, sameInstant);
        AcceptedLearningEvidence fail = evidence(AttemptPurpose.INDEPENDENT_TEST, LearningResult.FAIL, 0, sameInstant);

        ConceptProgress passFirst = projector.project(LEARNER, CONCEPT, List.of(pass, fail));
        ConceptProgress failFirst = projector.project(LEARNER, CONCEPT, List.of(fail, pass));

        assertEquals(passFirst, failFirst,
                "input order must never change the projected result");
        assertEquals(ConceptProgressProjector.EVIDENCE_ORDER.compare(pass, fail),
                -ConceptProgressProjector.EVIDENCE_ORDER.compare(fail, pass));
    }

    private AcceptedLearningEvidence evidence(AttemptPurpose purpose, LearningResult result, int hintLevel) {
        return evidence(purpose, result, hintLevel, NOW);
    }

    private AcceptedLearningEvidence evidence(
            AttemptPurpose purpose, LearningResult result, int hintLevel, Instant acceptedAt
    ) {
        return new AcceptedLearningEvidence(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), CONCEPT, LEARNER,
                result, purpose, hintLevel, List.of(), acceptedAt
        );
    }
}
