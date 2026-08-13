package cn.lunalhx.ai.kilnai.infrastructure.adapter.repository;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mapper
public interface SpikeStoreMapper {

    @Insert("""
            INSERT INTO learning_flows (
                id, learner_id, concept_id, contract_id, rubric_id, source_pack_id,
                status, stage, created_at, updated_at
            ) VALUES (
                #{id}, #{learnerId}, #{conceptId}, #{contractId}, #{rubricId}, #{sourcePackId},
                #{status}, #{stage}, #{createdAt}, #{createdAt}
            )
            """)
    void insertFlow(
            @Param("id") UUID id,
            @Param("learnerId") UUID learnerId,
            @Param("conceptId") UUID conceptId,
            @Param("contractId") UUID contractId,
            @Param("rubricId") UUID rubricId,
            @Param("sourcePackId") UUID sourcePackId,
            @Param("status") String status,
            @Param("stage") String stage,
            @Param("createdAt") Instant createdAt
    );

    @Select("""
            SELECT id, learner_id, concept_id, contract_id, rubric_id, source_pack_id,
                   status, stage, created_at
            FROM learning_flows
            WHERE id = #{flowId}
            """)
    Optional<FlowRow> findFlow(UUID flowId);

    @Update("""
            UPDATE learning_flows
            SET status = #{status}, stage = #{stage}, updated_at = #{updatedAt}
            WHERE id = #{flowId}
            """)
    void updateFlow(
            @Param("flowId") UUID flowId,
            @Param("status") String status,
            @Param("stage") String stage,
            @Param("updatedAt") Instant updatedAt
    );

    @Insert("""
            INSERT INTO learning_checkpoints (
                id, flow_id, thread_id, node_id, next_node_id, schema_version, blackboard, created_at
            ) VALUES (
                #{id}, #{flowId}, #{threadId}, #{nodeId}, #{nextNodeId}, #{schemaVersion},
                CAST(#{blackboardJson} AS JSONB), #{createdAt}
            )
            """)
    void insertCheckpoint(CheckpointRow row);

    @Select("""
            SELECT id, flow_id, thread_id, node_id, next_node_id, schema_version,
                   blackboard::text AS blackboard_json, created_at
            FROM learning_checkpoints
            WHERE flow_id = #{flowId}
            ORDER BY created_at DESC
            """)
    List<CheckpointRow> listCheckpoints(UUID flowId);

    @Insert("""
            INSERT INTO learner_interactions (
                id, flow_id, interaction_version, status, stage, visible_content,
                allowed_event_kinds, created_at
            ) VALUES (
                #{id}, #{flowId}, #{interactionVersion}, #{status}, #{stage}, #{visibleContent},
                CAST(#{allowedEventKindsJson} AS JSONB), #{createdAt}
            )
            """)
    void insertInteraction(InteractionRow row);

    @Select("""
            SELECT id, flow_id, interaction_version, status, stage, visible_content,
                   allowed_event_kinds::text AS allowed_event_kinds_json, created_at
            FROM learner_interactions
            WHERE flow_id = #{flowId}
            ORDER BY interaction_version DESC
            LIMIT 1
            """)
    Optional<InteractionRow> latestInteraction(UUID flowId);

    @Insert("""
            INSERT INTO artifacts (id, artifact_type, schema_version, visibility, content_hash, payload, created_at)
            VALUES (
                #{id}, #{artifactType}, #{schemaVersion}, #{visibility}, #{contentHash},
                CAST(#{payloadJson} AS JSONB), #{createdAt}
            )
            """)
    void insertArtifact(
            @Param("id") UUID id,
            @Param("artifactType") String artifactType,
            @Param("schemaVersion") int schemaVersion,
            @Param("visibility") String visibility,
            @Param("contentHash") String contentHash,
            @Param("payloadJson") String payloadJson,
            @Param("createdAt") Instant createdAt
    );

    @Select("SELECT payload::text FROM artifacts WHERE id = #{artifactId}")
    Optional<String> findArtifactPayload(UUID artifactId);

    @Insert("""
            INSERT INTO task_attempts (id, flow_id, task_package_id, purpose, status, created_at)
            VALUES (#{id}, #{flowId}, #{taskPackageId}, #{purpose}, #{status}, #{createdAt})
            """)
    void insertAttempt(
            @Param("id") UUID id,
            @Param("flowId") UUID flowId,
            @Param("taskPackageId") UUID taskPackageId,
            @Param("purpose") String purpose,
            @Param("status") String status,
            @Param("createdAt") Instant createdAt
    );

    @Insert("""
            INSERT INTO accepted_learning_evidence (
                id, task_attempt_id, flow_id, concept_id, learner_id, result,
                attempt_purpose, highest_hint_level, assistance_trace, accepted_at
            ) VALUES (
                #{id}, #{taskAttemptId}, #{flowId}, #{conceptId}, #{learnerId}, #{result},
                #{attemptPurpose}, #{highestHintLevel}, CAST(#{assistanceTraceJson} AS JSONB), #{acceptedAt}
            )
            ON CONFLICT (task_attempt_id) DO NOTHING
            """)
    void insertEvidence(EvidenceRow row);

    @Select("SELECT 1 FROM accepted_learning_evidence WHERE task_attempt_id = #{attemptId}")
    Optional<Integer> evidenceExists(UUID attemptId);

    @Insert("""
            INSERT INTO concept_progress (
                learner_id, concept_id, current_milestone, highest_milestone, current_stage, updated_at
            ) VALUES (
                #{learnerId}, #{conceptId}, #{currentMilestone}, #{highestMilestone}, #{currentStage}, #{updatedAt}
            )
            ON CONFLICT (learner_id, concept_id) DO UPDATE SET
                current_milestone = EXCLUDED.current_milestone,
                highest_milestone = EXCLUDED.highest_milestone,
                current_stage = EXCLUDED.current_stage,
                updated_at = EXCLUDED.updated_at
            """)
    void upsertProgress(
            @Param("learnerId") UUID learnerId,
            @Param("conceptId") UUID conceptId,
            @Param("currentMilestone") String currentMilestone,
            @Param("highestMilestone") String highestMilestone,
            @Param("currentStage") String currentStage,
            @Param("updatedAt") Instant updatedAt
    );

    @Insert("""
            INSERT INTO graph_run_traces (id, flow_id, schema_version, private_payload, public_payload, created_at)
            VALUES (
                #{id}, #{flowId}, 1, CAST(#{privateJson} AS JSONB), CAST(#{publicJson} AS JSONB), #{createdAt}
            )
            """)
    void insertTrace(
            @Param("id") UUID id,
            @Param("flowId") UUID flowId,
            @Param("privateJson") String privateJson,
            @Param("publicJson") String publicJson,
            @Param("createdAt") Instant createdAt
    );

    @Select("""
            SELECT public_payload::text AS public_json, private_payload::text AS private_json
            FROM graph_run_traces
            WHERE flow_id = #{flowId}
            ORDER BY created_at ASC
            """)
    List<TraceRow> listTraces(UUID flowId);

    @Select("""
            SELECT public_payload::text AS public_json, private_payload::text AS private_json
            FROM graph_run_traces
            WHERE flow_id = #{flowId}
            ORDER BY created_at DESC
            LIMIT 1
            """)
    Optional<TraceRow> latestTrace(UUID flowId);

    @Insert("""
            INSERT INTO processed_commands (
                idempotency_key, request_hash, flow_id, status_code, response_body, created_at
            ) VALUES (
                #{idempotencyKey}, #{requestHash}, #{flowId}, #{statusCode},
                CAST(#{responseJson} AS JSONB), #{createdAt}
            )
            ON CONFLICT (idempotency_key) DO NOTHING
            """)
    void insertCommand(CommandRow row);

    @Select("""
            SELECT idempotency_key, request_hash, flow_id, status_code,
                   response_body::text AS response_json, created_at
            FROM processed_commands
            WHERE idempotency_key = #{idempotencyKey}
            """)
    Optional<CommandRow> findCommand(UUID idempotencyKey);

    record FlowRow(
            UUID id,
            UUID learnerId,
            UUID conceptId,
            UUID contractId,
            UUID rubricId,
            UUID sourcePackId,
            String status,
            String stage,
            Instant createdAt
    ) {
    }

    record CheckpointRow(
            UUID id,
            UUID flowId,
            String threadId,
            String nodeId,
            String nextNodeId,
            int schemaVersion,
            String blackboardJson,
            Instant createdAt
    ) {
    }

    record InteractionRow(
            UUID id,
            UUID flowId,
            int interactionVersion,
            String status,
            String stage,
            String visibleContent,
            String allowedEventKindsJson,
            Instant createdAt
    ) {
    }

    record EvidenceRow(
            UUID id,
            UUID taskAttemptId,
            UUID flowId,
            UUID conceptId,
            UUID learnerId,
            String result,
            String attemptPurpose,
            int highestHintLevel,
            String assistanceTraceJson,
            Instant acceptedAt
    ) {
    }

    record TraceRow(String publicJson, String privateJson) {
    }

    record CommandRow(
            UUID idempotencyKey,
            String requestHash,
            UUID flowId,
            int statusCode,
            String responseJson,
            Instant createdAt
    ) {
    }
}
