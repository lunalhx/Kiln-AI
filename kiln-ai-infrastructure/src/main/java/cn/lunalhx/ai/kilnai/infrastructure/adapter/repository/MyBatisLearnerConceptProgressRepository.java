package cn.lunalhx.ai.kilnai.infrastructure.adapter.repository;

import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.LearnerConceptProgressRepository;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.LearnerConceptProgress;

import java.util.Optional;
import java.util.UUID;

public class MyBatisLearnerConceptProgressRepository implements LearnerConceptProgressRepository {

    private final LearnerConceptProgressMapper mapper;

    public MyBatisLearnerConceptProgressRepository(LearnerConceptProgressMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<LearnerConceptProgress> findByUserIdAndConceptId(UUID userId, UUID conceptId) {
        return mapper.findByUserIdAndConceptId(userId, conceptId).map(row -> LearnerConceptProgress.restore(
                row.userId(), row.conceptId(), row.state(), row.hasIndependentSuccess(),
                row.hasDelayedIndependentSuccess(), row.hasTransferSuccess(),
                row.lastIndependentSuccessAt(), row.lastFailureAt(), row.updatedAt()
        ));
    }

    @Override
    public void save(LearnerConceptProgress progress) {
        mapper.upsert(progress);
    }
}
