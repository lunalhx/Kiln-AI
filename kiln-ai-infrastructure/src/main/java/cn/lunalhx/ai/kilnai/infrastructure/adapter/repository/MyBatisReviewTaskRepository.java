package cn.lunalhx.ai.kilnai.infrastructure.adapter.repository;

import cn.lunalhx.ai.kilnai.domain.review.adapter.port.ReviewTaskRepository;
import cn.lunalhx.ai.kilnai.domain.review.model.entity.ReviewTask;

public class MyBatisReviewTaskRepository implements ReviewTaskRepository {

    private final ReviewTaskMapper mapper;

    public MyBatisReviewTaskRepository(ReviewTaskMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(ReviewTask reviewTask) {
        mapper.insert(reviewTask);
    }
}
