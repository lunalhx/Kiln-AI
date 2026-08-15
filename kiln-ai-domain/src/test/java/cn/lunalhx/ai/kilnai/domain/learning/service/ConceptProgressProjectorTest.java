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
    void oneToThreeReviewPassesKeepCurrentMilestoneIndependent() {
        ConceptProgress progress = projector.project(LEARNER, CONCEPT, List.of(
                evidence(AttemptPurpose.INDEPENDENT_TEST, LearningResult.PASS, 0, NOW),
                evidence(AttemptPurpose.REVIEW, LearningResult.PASS, 0, NOW.plusSeconds(60)),
                evidence(AttemptPurpose.REVIEW, LearningResult.PASS, 0, NOW.plusSeconds(120)),
                evidence(AttemptPurpose.REVIEW, LearningResult.PASS, 0, NOW.plusSeconds(180))
        ));

        assertEquals(MasteryMilestone.INDEPENDENT, progress.currentMilestone(),
                "three consecutive Review passes must not advance the milestone");
        assertEquals(MasteryMilestone.INDEPENDENT, progress.highestMilestoneReached());
        assertEquals(LearningStage.DELAYED_REVIEW, progress.currentStage());
    }

    @Test
    void fourConsecutiveReviewPassesProjectDurable() {
        ConceptProgress progress = projector.project(LEARNER, CONCEPT, List.of(
                evidence(AttemptPurpose.INDEPENDENT_TEST, LearningResult.PASS, 0, NOW),
                reviewPass(NOW.plusSeconds(60)),
                reviewPass(NOW.plusSeconds(120)),
                reviewPass(NOW.plusSeconds(180)),
                reviewPass(NOW.plusSeconds(240))
        ));

        assertEquals(MasteryMilestone.DURABLE, progress.currentMilestone());
        assertEquals(MasteryMilestone.DURABLE, progress.highestMilestoneReached());
        assertEquals(LearningStage.DELAYED_REVIEW, progress.currentStage());
    }

    @Test
    void aHintedReviewPassNeverCountsAsAQualifyingReviewSuccess() {
        ConceptProgress progress = projector.project(LEARNER, CONCEPT, List.of(
                evidence(AttemptPurpose.INDEPENDENT_TEST, LearningResult.PASS, 0, NOW),
                evidence(AttemptPurpose.REVIEW, LearningResult.PASS, 1, NOW.plusSeconds(60))
        ));

        assertEquals(MasteryMilestone.INDEPENDENT, progress.currentMilestone(),
                "an assisted Review pass must not count toward Durable");
        assertEquals(MasteryMilestone.INDEPENDENT, progress.highestMilestoneReached());
    }

    @Test
    void aFreshIndependentPassRestartsTheReviewSuccessCount() {
        ConceptProgress progress = projector.project(LEARNER, CONCEPT, List.of(
                evidence(AttemptPurpose.INDEPENDENT_TEST, LearningResult.PASS, 0, NOW),
                reviewPass(NOW.plusSeconds(60)),
                reviewPass(NOW.plusSeconds(120)),
                reviewPass(NOW.plusSeconds(180)),
                evidence(AttemptPurpose.INDEPENDENT_TEST, LearningResult.PASS, 0, NOW.plusSeconds(240)),
                reviewPass(NOW.plusSeconds(300))
        ));

        assertEquals(MasteryMilestone.INDEPENDENT, progress.currentMilestone(),
                "a fresh Independent pass must reset the consecutive Review count");
        assertEquals(MasteryMilestone.INDEPENDENT, progress.highestMilestoneReached());
    }

    @Test
    void aReviewPassWithoutAnIndependentFoundationNeverAdvancesTheMilestone() {
        ConceptProgress progress = projector.project(LEARNER, CONCEPT, List.of(
                reviewPass(NOW.plusSeconds(60)),
                reviewPass(NOW.plusSeconds(120)),
                reviewPass(NOW.plusSeconds(180)),
                reviewPass(NOW.plusSeconds(240))
        ));

        assertEquals(MasteryMilestone.UNASSESSED, progress.currentMilestone(),
                "Review passes alone cannot establish Independent, let alone Durable");
        assertEquals(MasteryMilestone.UNASSESSED, progress.highestMilestoneReached());
    }

    @Test
    void aFallFromIndependentEndsTheConsecutiveReviewRun() {
        ConceptProgress progress = projector.project(LEARNER, CONCEPT, List.of(
                evidence(AttemptPurpose.INDEPENDENT_TEST, LearningResult.PASS, 0, NOW),
                reviewPass(NOW.plusSeconds(60)),
                reviewPass(NOW.plusSeconds(120)),
                evidence(AttemptPurpose.INDEPENDENT_TEST, LearningResult.FAIL, 0, NOW.plusSeconds(180)),
                reviewPass(NOW.plusSeconds(240)),
                reviewPass(NOW.plusSeconds(300)),
                reviewPass(NOW.plusSeconds(360)),
                reviewPass(NOW.plusSeconds(420))
        ));

        assertEquals(MasteryMilestone.LEARNING, progress.currentMilestone(),
                "a verified no-hint failure ends the consecutive run even if four Review passes follow");
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
    void aNoHintReviewFailDropsCurrentMilestoneToLearningAndPreservesHighest() {
        ConceptProgress progress = projector.project(LEARNER, CONCEPT, List.of(
                evidence(AttemptPurpose.INDEPENDENT_TEST, LearningResult.PASS, 0, NOW),
                evidence(AttemptPurpose.REVIEW, LearningResult.FAIL, 0, NOW.plusSeconds(60))
        ));

        assertEquals(MasteryMilestone.LEARNING, progress.currentMilestone(),
                "a qualifying no-hint Review failure must drop Current Milestone to Learning");
        assertEquals(MasteryMilestone.INDEPENDENT, progress.highestMilestoneReached(),
                "a Review failure must never erase the highest milestone reached");
        assertEquals(LearningStage.LEARNING_AND_PRACTICE, progress.currentStage());
    }

    @Test
    void aReviewFailAfterDurablePreservesHighestDurableWhileCurrentFalls() {
        ConceptProgress progress = projector.project(LEARNER, CONCEPT, List.of(
                evidence(AttemptPurpose.INDEPENDENT_TEST, LearningResult.PASS, 0, NOW),
                reviewPass(NOW.plusSeconds(60)),
                reviewPass(NOW.plusSeconds(120)),
                reviewPass(NOW.plusSeconds(180)),
                reviewPass(NOW.plusSeconds(240)),
                evidence(AttemptPurpose.REVIEW, LearningResult.FAIL, 0, NOW.plusSeconds(300))
        ));

        assertEquals(MasteryMilestone.LEARNING, progress.currentMilestone());
        assertEquals(MasteryMilestone.DURABLE, progress.highestMilestoneReached(),
                "a failure after a historical Durable milestone must keep Highest at Durable");
        assertEquals(LearningStage.LEARNING_AND_PRACTICE, progress.currentStage());
    }

    @Test
    void aReviewFailEndsTheConsecutiveSuccessRunEvenBeforeFourPasses() {
        ConceptProgress progress = projector.project(LEARNER, CONCEPT, List.of(
                evidence(AttemptPurpose.INDEPENDENT_TEST, LearningResult.PASS, 0, NOW),
                reviewPass(NOW.plusSeconds(60)),
                reviewPass(NOW.plusSeconds(120)),
                evidence(AttemptPurpose.REVIEW, LearningResult.FAIL, 0, NOW.plusSeconds(180)),
                reviewPass(NOW.plusSeconds(240)),
                reviewPass(NOW.plusSeconds(300)),
                reviewPass(NOW.plusSeconds(360)),
                reviewPass(NOW.plusSeconds(420))
        ));

        assertEquals(MasteryMilestone.LEARNING, progress.currentMilestone(),
                "a qualifying Review failure must end the consecutive run even if four Review passes follow");
        assertEquals(MasteryMilestone.INDEPENDENT, progress.highestMilestoneReached());
    }

    @Test
    void aHintedReviewFailIsNotAQualifyingFailure() {
        ConceptProgress progress = projector.project(LEARNER, CONCEPT, List.of(
                evidence(AttemptPurpose.INDEPENDENT_TEST, LearningResult.PASS, 0, NOW),
                evidence(AttemptPurpose.REVIEW, LearningResult.FAIL, 1, NOW.plusSeconds(60))
        ));

        assertEquals(MasteryMilestone.INDEPENDENT, progress.currentMilestone(),
                "an assisted Review failure is not conclusive and must not drop the milestone");
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

    private AcceptedLearningEvidence reviewPass(Instant acceptedAt) {
        return evidence(AttemptPurpose.REVIEW, LearningResult.PASS, 0, acceptedAt);
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
