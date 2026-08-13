package cn.lunalhx.ai.kilnai.domain.gate;

import cn.lunalhx.ai.kilnai.domain.artifact.EvidenceCandidate;

import java.util.List;

public final class EvidenceCandidateGatePolicy implements GatePolicy<EvidenceCandidate> {

    @Override
    public GateResult<EvidenceCandidate> evaluate(EvidenceCandidate candidate, GateContext context) {
        if (candidate.result() == null) {
            return GateResult.rejected(List.of(new GateViolation("evidence.inconclusive", "missing result")));
        }
        if ("conflict".equals(candidate.rationale())) {
            return GateResult.rejected(List.of(new GateViolation("evidence.conflict", "conflicting evaluations")));
        }
        return GateResult.passed(candidate);
    }
}
