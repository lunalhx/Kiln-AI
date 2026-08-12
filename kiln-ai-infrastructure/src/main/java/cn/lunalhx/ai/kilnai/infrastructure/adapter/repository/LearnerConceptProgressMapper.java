package cn.lunalhx.ai.kilnai.infrastructure.adapter.repository;

import cn.lunalhx.ai.kilnai.domain.learning.model.entity.LearnerConceptProgress;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;
import java.util.UUID;

@Mapper
public interface LearnerConceptProgressMapper {

    @Select("""
            SELECT user_id, concept_id, state, has_independent_success,
                   has_delayed_independent_success, has_transfer_success,
                   last_independent_success_at, last_failure_at, updated_at
            FROM learner_concept_progress
            WHERE user_id = #{userId} AND concept_id = #{conceptId}
            """)
    Optional<ProgressRow> findByUserIdAndConceptId(UUID userId, UUID conceptId);

    @Insert("""
            INSERT INTO learner_concept_progress (
                user_id, concept_id, state, has_independent_success,
                has_delayed_independent_success, has_transfer_success,
                last_independent_success_at, last_failure_at, updated_at
            ) VALUES (
                #{userId}, #{conceptId}, #{state}, #{hasIndependentSuccess},
                #{hasDelayedIndependentSuccess}, #{hasTransferSuccess},
                #{lastIndependentSuccessAt}, #{lastFailureAt}, #{updatedAt}
            ) ON CONFLICT (user_id, concept_id) DO UPDATE SET
                state = EXCLUDED.state,
                has_independent_success = EXCLUDED.has_independent_success,
                has_delayed_independent_success = EXCLUDED.has_delayed_independent_success,
                has_transfer_success = EXCLUDED.has_transfer_success,
                last_independent_success_at = EXCLUDED.last_independent_success_at,
                last_failure_at = EXCLUDED.last_failure_at,
                updated_at = EXCLUDED.updated_at
            """)
    void upsert(LearnerConceptProgress progress);
}
