package cn.lunalhx.ai.kilnai.domain.review.adapter.port;

import cn.lunalhx.ai.kilnai.domain.review.model.entity.ReviewTask;

public interface ReviewTaskRepository {

    void save(ReviewTask reviewTask);
}
