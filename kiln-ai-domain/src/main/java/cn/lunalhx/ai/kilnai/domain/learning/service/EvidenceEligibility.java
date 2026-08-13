package cn.lunalhx.ai.kilnai.domain.learning.service;

import cn.lunalhx.ai.kilnai.domain.artifact.EvidenceCandidate;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptStatus;

public final class EvidenceEligibility {

    public boolean eligible(AttemptStatus attemptStatus, AttemptPurpose purpose, EvidenceCandidate candidate, boolean alreadyAccepted) {
        if (alreadyAccepted) {
            return false;
        }
        if (attemptStatus != AttemptStatus.SUBMITTED) {
            return false;
        }
        if (candidate == null || candidate.result() == null) {
            return false;
        }
        return purpose == AttemptPurpose.PRACTICE || purpose == AttemptPurpose.INDEPENDENT_TEST;
    }
}
