package cn.lunalhx.ai.kilnai.domain.learning.adapter.port;

import cn.lunalhx.ai.kilnai.domain.learning.model.entity.LearnerConceptProgress;

import java.util.Optional;
import java.util.UUID;

public interface LearnerConceptProgressRepository {

    Optional<LearnerConceptProgress> findByUserIdAndConceptId(UUID userId, UUID conceptId);

    void save(LearnerConceptProgress progress);
}
