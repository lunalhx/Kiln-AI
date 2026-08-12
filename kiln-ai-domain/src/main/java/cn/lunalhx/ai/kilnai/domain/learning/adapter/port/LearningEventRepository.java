package cn.lunalhx.ai.kilnai.domain.learning.adapter.port;

import cn.lunalhx.ai.kilnai.domain.learning.model.entity.LearningEvent;

public interface LearningEventRepository {

    void append(LearningEvent learningEvent);
}
