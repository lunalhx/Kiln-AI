package cn.lunalhx.ai.kilnai.infrastructure.adapter.repository;

import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.LearningEventRepository;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.LearningEvent;

public class MyBatisLearningEventRepository implements LearningEventRepository {

    private final LearningEventMapper mapper;

    public MyBatisLearningEventRepository(LearningEventMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void append(LearningEvent learningEvent) {
        mapper.insert(learningEvent);
    }
}
