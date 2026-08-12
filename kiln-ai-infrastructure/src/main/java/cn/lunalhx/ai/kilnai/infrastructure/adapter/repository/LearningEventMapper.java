package cn.lunalhx.ai.kilnai.infrastructure.adapter.repository;

import cn.lunalhx.ai.kilnai.domain.learning.model.entity.LearningEvent;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LearningEventMapper {

    @Insert("""
            INSERT INTO learning_events (
                id, user_id, concept_id, event_type, result, hint_level,
                is_delayed_review, is_transfer, confidence, error_tag, occurred_at, recorded_at
            ) VALUES (
                #{id}, #{userId}, #{conceptId}, #{evidence.eventType}, #{evidence.result}, #{evidence.hintLevel},
                #{evidence.delayedReview}, #{evidence.transfer}, #{confidence}, #{errorTag},
                #{evidence.occurredAt}, #{recordedAt}
            )
            """)
    void insert(LearningEvent learningEvent);
}
