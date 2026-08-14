package cn.lunalhx.ai.kilnai.domain.apply.port;

import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;

import java.util.List;

/**
 * The store of accepted Learning Evidence for the Apply reference. Evidence is
 * accepted at most once per Task Attempt: a duplicate accept for the same
 * task attempt is ignored.
 */
public interface EvidenceStorePort {

    void accept(AcceptedLearningEvidence evidence);

    List<AcceptedLearningEvidence> allEvidence();
}
