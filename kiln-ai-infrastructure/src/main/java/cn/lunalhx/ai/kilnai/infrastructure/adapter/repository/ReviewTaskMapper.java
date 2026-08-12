package cn.lunalhx.ai.kilnai.infrastructure.adapter.repository;

import cn.lunalhx.ai.kilnai.domain.review.model.entity.ReviewTask;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReviewTaskMapper {

    @Insert("""
            INSERT INTO review_tasks (id, user_id, concept_id, task_type, status, due_at, created_at)
            VALUES (#{id}, #{userId}, #{conceptId}, #{taskType}, #{status}, #{dueAt}, #{createdAt})
            """)
    void insert(ReviewTask reviewTask);
}
