package cn.lunalhx.ai.kilnai.domain.apply.model;

import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import org.junit.jupiter.api.Test;

import java.util.List;

import static cn.lunalhx.ai.kilnai.domain.apply.model.EquivalenceOutcome.CANNOT_DECIDE;
import static cn.lunalhx.ai.kilnai.domain.apply.model.EquivalenceOutcome.PROVEN_EQUIVALENT;
import static cn.lunalhx.ai.kilnai.domain.apply.model.EquivalenceOutcome.PROVEN_NOT_EQUIVALENT;
import static cn.lunalhx.ai.kilnai.domain.apply.model.FinalExpressionJudgment.EQUIVALENT;
import static cn.lunalhx.ai.kilnai.domain.apply.model.FinalExpressionJudgment.INCONCLUSIVE;
import static cn.lunalhx.ai.kilnai.domain.apply.model.FinalExpressionJudgment.NOT_EQUIVALENT;
import static cn.lunalhx.ai.kilnai.domain.apply.model.FinalExpressionJudgment.NOT_REQUESTED;
import static cn.lunalhx.ai.kilnai.domain.apply.model.RationaleJudgment.APPLICABLE;
import static cn.lunalhx.ai.kilnai.domain.apply.model.RationaleJudgment.CLEARLY_CONTRADICTORY;
import static cn.lunalhx.ai.kilnai.domain.apply.model.RationaleJudgment.NOT_APPLICABLE;
import static cn.lunalhx.ai.kilnai.domain.apply.model.RationaleJudgment.NOT_CLEARLY_CONTRADICTORY;
import static cn.lunalhx.ai.kilnai.domain.apply.model.RationaleJudgment.NOT_PROVIDED;
import static cn.lunalhx.ai.kilnai.domain.apply.model.RationaleJudgment.NON_SUBSTANTIVE;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResponseAssessmentDeciderTest {

    @Test
    void aProvenEquivalentDiagnosticPassesWithoutAnyModelJudgment() {
        AssessmentOutcome outcome = decide(
                AttemptPurpose.DIAGNOSTIC, PROVEN_EQUIVALENT, null, null);

        assertInstanceOf(AssessmentOutcome.Passed.class, outcome);
        assertNull(((AssessmentOutcome.Passed) outcome).assessment(),
                "no model judgment may be invoked for a proven result");
    }

    @Test
    void aProvenNotEquivalentDiagnosticFailsWhenTheRationaleIsNotApplicable() {
        AssessmentOutcome outcome = decide(
                AttemptPurpose.DIAGNOSTIC, PROVEN_NOT_EQUIVALENT,
                judgment(NOT_REQUESTED, NOT_APPLICABLE), null);

        assertInstanceOf(AssessmentOutcome.Failed.class, outcome);
    }

    @Test
    void anApplicableDiagnosticRationaleCannotOverrideAProvenNonEquivalentFinal() {
        AssessmentOutcome outcome = decide(
                AttemptPurpose.DIAGNOSTIC, PROVEN_NOT_EQUIVALENT,
                judgment(NOT_REQUESTED, APPLICABLE), null);

        assertInstanceOf(AssessmentOutcome.Failed.class, outcome);
    }

    @Test
    void anInconclusiveDiagnosticRationaleCannotOverrideAProvenNonEquivalentFinal() {
        AssessmentOutcome outcome = decide(
                AttemptPurpose.DIAGNOSTIC, PROVEN_NOT_EQUIVALENT,
                judgment(NOT_REQUESTED, RationaleJudgment.INCONCLUSIVE), null);

        assertInstanceOf(AssessmentOutcome.Failed.class, outcome);
    }

    @Test
    void cannotDecidePassesOnlyWhenAssessmentAndVerificationBothJudgeEquivalent() {
        AssessmentOutcome outcome = decide(
                AttemptPurpose.DIAGNOSTIC, CANNOT_DECIDE,
                judgment(EQUIVALENT, NOT_APPLICABLE), judgment(EQUIVALENT, NOT_APPLICABLE));

        assertInstanceOf(AssessmentOutcome.Passed.class, outcome);
    }

    @Test
    void cannotDecideWithDisagreeingJudgmentsIsUnconfirmedNeverFailedOrPassed() {
        AssessmentOutcome outcome = decide(
                AttemptPurpose.DIAGNOSTIC, CANNOT_DECIDE,
                judgment(EQUIVALENT, NOT_APPLICABLE), judgment(NOT_EQUIVALENT, NOT_APPLICABLE));

        assertInstanceOf(AssessmentOutcome.Unconfirmed.class, outcome);
    }

    @Test
    void cannotDecideWithBothNonEquivalentIsUnconfirmedNeverGuessedWrong() {
        AssessmentOutcome outcome = decide(
                AttemptPurpose.DIAGNOSTIC, CANNOT_DECIDE,
                judgment(NOT_EQUIVALENT, NOT_APPLICABLE), judgment(NOT_EQUIVALENT, NOT_APPLICABLE));

        assertInstanceOf(AssessmentOutcome.Unconfirmed.class, outcome);
    }

    @Test
    void cannotDecideWithAnInconclusiveModelJudgmentIsUnconfirmed() {
        AssessmentOutcome outcome = decide(
                AttemptPurpose.DIAGNOSTIC, CANNOT_DECIDE,
                judgment(EQUIVALENT, NOT_APPLICABLE), judgment(INCONCLUSIVE, NOT_APPLICABLE));

        assertInstanceOf(AssessmentOutcome.Unconfirmed.class, outcome);
    }

    @Test
    void cannotDecideWithoutAResponseVerificationJudgmentIsUnconfirmed() {
        AssessmentOutcome outcome = decide(
                AttemptPurpose.DIAGNOSTIC, CANNOT_DECIDE,
                judgment(EQUIVALENT, NOT_APPLICABLE), null);

        assertInstanceOf(AssessmentOutcome.Unconfirmed.class, outcome);
    }

    @Test
    void anApplicableRationaleCannotRescueCannotDecideJudgmentsAgreeingOnNonEquivalent() {
        AssessmentOutcome outcome = decide(
                AttemptPurpose.DIAGNOSTIC, CANNOT_DECIDE,
                judgment(NOT_EQUIVALENT, APPLICABLE), judgment(NOT_EQUIVALENT, APPLICABLE));

        assertInstanceOf(AssessmentOutcome.Unconfirmed.class, outcome);
    }

    @Test
    void anOmittedOrNonSubstantiveIndependentRationaleDoesNotBlock() {
        assertInstanceOf(AssessmentOutcome.Passed.class, decide(
                AttemptPurpose.INDEPENDENT_TEST, PROVEN_EQUIVALENT,
                judgment(NOT_REQUESTED, NOT_PROVIDED), null));
        assertInstanceOf(AssessmentOutcome.Passed.class, decide(
                AttemptPurpose.INDEPENDENT_TEST, PROVEN_EQUIVALENT,
                judgment(NOT_REQUESTED, NON_SUBSTANTIVE), null));
        assertInstanceOf(AssessmentOutcome.Passed.class, decide(
                AttemptPurpose.INDEPENDENT_TEST, PROVEN_EQUIVALENT,
                judgment(NOT_REQUESTED, NOT_CLEARLY_CONTRADICTORY), null));
    }

    @Test
    void aClearlyContradictoryIndependentRationaleBlocksEvidence() {
        AssessmentOutcome outcome = decide(
                AttemptPurpose.INDEPENDENT_TEST, PROVEN_EQUIVALENT,
                judgment(NOT_REQUESTED, CLEARLY_CONTRADICTORY), null);

        assertInstanceOf(AssessmentOutcome.Blocked.class, outcome);
    }

    @Test
    void aClearlyContradictoryRationaleBlocksEvidenceEvenWhenCannotDecideJudgmentsBothPass() {
        AssessmentOutcome outcome = decide(
                AttemptPurpose.INDEPENDENT_TEST, CANNOT_DECIDE,
                judgment(EQUIVALENT, CLEARLY_CONTRADICTORY), judgment(EQUIVALENT, CLEARLY_CONTRADICTORY));

        assertInstanceOf(AssessmentOutcome.Blocked.class, outcome);
    }

    @Test
    void cannotDecideWithDisagreementIsInconclusiveForAnIndependentSubmission() {
        AssessmentOutcome outcome = decide(
                AttemptPurpose.INDEPENDENT_TEST, CANNOT_DECIDE,
                judgment(EQUIVALENT, NOT_PROVIDED), judgment(NOT_EQUIVALENT, NOT_PROVIDED));

        assertInstanceOf(AssessmentOutcome.Inconclusive.class, outcome);
    }

    @Test
    void anInconclusiveIndependentRationaleBlocksNeitherWayAndStaysInconclusive() {
        AssessmentOutcome outcome = decide(
                AttemptPurpose.INDEPENDENT_TEST, PROVEN_EQUIVALENT,
                judgment(NOT_REQUESTED, RationaleJudgment.INCONCLUSIVE), null);

        assertInstanceOf(AssessmentOutcome.Inconclusive.class, outcome);
    }

    @Test
    void aProvenNonEquivalentIndependentFailsWithoutConsultingTheRationale() {
        AssessmentOutcome outcome = decide(
                AttemptPurpose.INDEPENDENT_TEST, PROVEN_NOT_EQUIVALENT,
                judgment(NOT_REQUESTED, CLEARLY_CONTRADICTORY), null);

        assertInstanceOf(AssessmentOutcome.Failed.class, outcome);
    }

    @Test
    void aModelCannotOverrideAProvenDeterministicNonEquivalence() {
        AssessmentOutcome outcome = decide(
                AttemptPurpose.INDEPENDENT_TEST, PROVEN_NOT_EQUIVALENT,
                judgment(EQUIVALENT, NOT_PROVIDED), null);

        assertInstanceOf(AssessmentOutcome.Failed.class, outcome,
                "a proven deterministic result is never overridden by a model judgment");
    }

    @Test
    void theOutcomeRetainsTheClosedTypedContractsForAudit() {
        ResponseAssessment assessment = judgment(EQUIVALENT, NOT_PROVIDED);
        ResponseAssessment verification = judgment(EQUIVALENT, NOT_PROVIDED);
        AssessmentOutcome outcome = decide(
                AttemptPurpose.INDEPENDENT_TEST, CANNOT_DECIDE, assessment, verification);

        AssessmentOutcome.Passed passed = (AssessmentOutcome.Passed) outcome;
        assertSame(assessment, passed.assessment());
        assertSame(verification, passed.verification());
    }

    @Test
    void aMissingModelJudgmentOnAProvenNonEquivalentDiagnosticFailsWithoutAnNpe() {
        AssessmentOutcome outcome = decide(
                AttemptPurpose.DIAGNOSTIC, PROVEN_NOT_EQUIVALENT, null, null);

        assertInstanceOf(AssessmentOutcome.Failed.class, outcome,
                "a missing model judgment is handled as a conclusive Diagnostic gap and never an NPE");
    }

    @Test
    void aContradictionJudgmentIsRejectedForADiagnostic() {
        assertThrows(IllegalArgumentException.class, () -> decide(
                AttemptPurpose.DIAGNOSTIC, PROVEN_NOT_EQUIVALENT,
                judgment(NOT_REQUESTED, CLEARLY_CONTRADICTORY), null));
    }

    @Test
    void anApplicableOrNotApplicableJudgmentIsRejectedForAnIndependentTest() {
        assertThrows(IllegalArgumentException.class, () -> decide(
                AttemptPurpose.INDEPENDENT_TEST, PROVEN_EQUIVALENT,
                judgment(NOT_REQUESTED, APPLICABLE), null));
        assertThrows(IllegalArgumentException.class, () -> decide(
                AttemptPurpose.INDEPENDENT_TEST, PROVEN_EQUIVALENT,
                judgment(NOT_REQUESTED, NOT_APPLICABLE), null));
    }

    @Test
    void aProvenEquivalentPracticePassesWithAnOmittedRationale() {
        AssessmentOutcome outcome = decide(
                AttemptPurpose.PRACTICE, PROVEN_EQUIVALENT,
                judgment(NOT_REQUESTED, NOT_PROVIDED), null);

        assertInstanceOf(AssessmentOutcome.Passed.class, outcome);
    }

    @Test
    void aProvenNonEquivalentPracticeFailsConclusively() {
        AssessmentOutcome outcome = decide(
                AttemptPurpose.PRACTICE, PROVEN_NOT_EQUIVALENT,
                judgment(NOT_REQUESTED, NOT_PROVIDED), null);

        assertInstanceOf(AssessmentOutcome.Failed.class, outcome);
    }

    @Test
    void aClearlyContradictoryPracticeRationaleOverACorrectAnswerIsBlockedEvidence() {
        AssessmentOutcome outcome = decide(
                AttemptPurpose.PRACTICE, PROVEN_EQUIVALENT,
                judgment(NOT_REQUESTED, CLEARLY_CONTRADICTORY), null);

        assertInstanceOf(AssessmentOutcome.Blocked.class, outcome,
                "the Practice submission flow maps this conclusive signal to a FAIL, never to a pass");
    }

    @Test
    void anInconclusivePracticeJudgmentOnCannotDecideIsInconclusive() {
        AssessmentOutcome outcome = decide(
                AttemptPurpose.PRACTICE, CANNOT_DECIDE,
                judgment(EQUIVALENT, NOT_PROVIDED), judgment(NOT_EQUIVALENT, NOT_PROVIDED));

        assertInstanceOf(AssessmentOutcome.Inconclusive.class, outcome);
    }

    @Test
    void anApplicableOrNotApplicableJudgmentIsRejectedForAPractice() {
        assertThrows(IllegalArgumentException.class, () -> decide(
                AttemptPurpose.PRACTICE, PROVEN_EQUIVALENT,
                judgment(NOT_REQUESTED, APPLICABLE), null));
        assertThrows(IllegalArgumentException.class, () -> decide(
                AttemptPurpose.PRACTICE, PROVEN_EQUIVALENT,
                judgment(NOT_REQUESTED, NOT_APPLICABLE), null));
    }

    private AssessmentOutcome decide(
            AttemptPurpose purpose,
            EquivalenceOutcome deterministic,
            ResponseAssessment assessment,
            ResponseAssessment verification
    ) {
        return ResponseAssessmentDecider.decide(
                new ResponseAssessmentContext("task", "expected", "confirmed", "raw", "",
                        purpose, deterministic),
                assessment, verification);
    }

    private ResponseAssessment judgment(FinalExpressionJudgment finalJudgment, RationaleJudgment rationale) {
        return new ResponseAssessment(ResponseAssessment.SCHEMA, finalJudgment, rationale, List.of());
    }
}
