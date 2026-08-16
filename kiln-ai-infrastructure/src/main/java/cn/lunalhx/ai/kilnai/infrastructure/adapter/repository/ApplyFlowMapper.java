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
public interface ApplyFlowMapper {

    @Insert("""
            INSERT INTO apply_flows (id, learner_id, concept_id, status, stage, created_at)
            VALUES (#{id}, #{learnerId}, #{conceptId}, #{status}, #{stage}, #{createdAt})
            """)
    void insertFlow(
            @Param("id") UUID id,
            @Param("learnerId") UUID learnerId,
            @Param("conceptId") UUID conceptId,
            @Param("status") String status,
            @Param("stage") String stage,
            @Param("createdAt") Instant createdAt
    );

    @Select("""
            SELECT id, learner_id, concept_id, status, stage, created_at
            FROM apply_flows
            WHERE id = #{flowId}
            """)
    Optional<ApplyFlowRow> findFlow(UUID flowId);

    @Insert("""
            INSERT INTO apply_interactions (
                id, flow_id, interaction_version, status, stage, attempt_id, attempt_purpose,
                learner_projection, learner_message, teaching_projection, hint, assistance_consent, created_at
            ) VALUES (
                #{id}, #{flowId}, #{interactionVersion}, #{status}, #{stage}, #{attemptId},
                #{attemptPurpose}, CAST(#{learnerProjectionJson} AS JSONB), #{learnerMessage},
                CAST(#{teachingProjectionJson} AS JSONB), CAST(#{hintJson} AS JSONB),
                CAST(#{assistanceConsentJson} AS JSONB), #{createdAt}
            )
            ON CONFLICT (flow_id, interaction_version) DO NOTHING
            """)
    void insertInteraction(InteractionRow row);

    @Select("""
            SELECT id, flow_id, interaction_version, status, stage, attempt_id, attempt_purpose,
                   learner_projection::text AS learner_projection_json, learner_message,
                   teaching_projection::text AS teaching_projection_json,
                   hint::text AS hint_json,
                   assistance_consent::text AS assistance_consent_json, created_at
            FROM apply_interactions
            WHERE flow_id = #{flowId}
            ORDER BY interaction_version DESC
            LIMIT 1
            """)
    Optional<InteractionRow> latestInteraction(UUID flowId);

    @Insert("""
            INSERT INTO apply_checkpoints (id, flow_id, interaction_version, created_at)
            VALUES (#{id}, #{flowId}, #{interactionVersion}, #{createdAt})
            """)
    void insertCheckpoint(CheckpointRow row);

    @Select("""
            SELECT id, flow_id, interaction_version, created_at
            FROM apply_checkpoints
            WHERE flow_id = #{flowId}
            ORDER BY created_at DESC
            LIMIT 1
            """)
    Optional<CheckpointRow> latestCheckpoint(UUID flowId);

    @Insert("""
            INSERT INTO apply_exposures (flow_id, task_package_id, task_fingerprint, solution_fingerprint, created_at)
            VALUES (#{flowId}, #{taskPackageId}, #{taskFingerprint}, #{solutionFingerprint}, #{createdAt})
            ON CONFLICT (flow_id, task_package_id) DO NOTHING
            """)
    void recordExposure(
            @Param("flowId") UUID flowId,
            @Param("taskPackageId") UUID taskPackageId,
            @Param("taskFingerprint") String taskFingerprint,
            @Param("solutionFingerprint") String solutionFingerprint,
            @Param("createdAt") Instant createdAt
    );

    @Select("SELECT task_fingerprint FROM apply_exposures WHERE flow_id = #{flowId} ORDER BY created_at ASC")
    List<String> exposedTaskFingerprints(UUID flowId);

    @Select("SELECT solution_fingerprint FROM apply_exposures WHERE flow_id = #{flowId} ORDER BY created_at ASC")
    List<String> exposedSolutionFingerprints(UUID flowId);

    @Insert("""
            INSERT INTO apply_example_exposures (flow_id, example_fingerprint, created_at)
            VALUES (#{flowId}, #{exampleFingerprint}, #{createdAt})
            ON CONFLICT (flow_id, example_fingerprint) DO NOTHING
            """)
    void recordExampleExposure(
            @Param("flowId") UUID flowId,
            @Param("exampleFingerprint") String exampleFingerprint,
            @Param("createdAt") Instant createdAt
    );

    @Select("""
            SELECT example_fingerprint
            FROM apply_example_exposures
            WHERE flow_id = #{flowId}
            ORDER BY created_at ASC
            """)
    List<String> exposedExampleFingerprints(UUID flowId);

    @Insert("""
            INSERT INTO apply_hint_ladder_exposures (flow_id, ladder_fingerprint, created_at)
            VALUES (#{flowId}, #{ladderFingerprint}, #{createdAt})
            ON CONFLICT (flow_id, ladder_fingerprint) DO NOTHING
            """)
    void recordHintLadderExposure(
            @Param("flowId") UUID flowId,
            @Param("ladderFingerprint") String ladderFingerprint,
            @Param("createdAt") Instant createdAt
    );

    @Select("""
            SELECT ladder_fingerprint
            FROM apply_hint_ladder_exposures
            WHERE flow_id = #{flowId}
            ORDER BY created_at ASC
            """)
    List<String> exposedHintLadderFingerprints(UUID flowId);

    @Insert("""
            INSERT INTO apply_revealed_solution_exposures (flow_id, reveal_fingerprint, created_at)
            VALUES (#{flowId}, #{revealFingerprint}, #{createdAt})
            ON CONFLICT (flow_id, reveal_fingerprint) DO NOTHING
            """)
    void recordRevealedSolutionExposure(
            @Param("flowId") UUID flowId,
            @Param("revealFingerprint") String revealFingerprint,
            @Param("createdAt") Instant createdAt
    );

    @Select("""
            SELECT reveal_fingerprint
            FROM apply_revealed_solution_exposures
            WHERE flow_id = #{flowId}
            ORDER BY created_at ASC
            """)
    List<String> exposedRevealedSolutionFingerprints(UUID flowId);

    @Insert("""
            INSERT INTO apply_explain_artifacts (id, flow_id, artifact, created_at)
            VALUES (#{id}, #{flowId}, CAST(#{artifactJson} AS JSONB), #{createdAt})
            """)
    void insertExplainArtifact(
            @Param("id") UUID id,
            @Param("flowId") UUID flowId,
            @Param("artifactJson") String artifactJson,
            @Param("createdAt") Instant createdAt
    );

    @Select("""
            SELECT artifact::text AS artifact_json
            FROM apply_explain_artifacts
            WHERE id = #{artifactId}
            """)
    Optional<ExplainArtifactRow> findExplainArtifact(UUID artifactId);

    @Insert("""
            INSERT INTO apply_commands (idempotency_key, request_hash, flow_id, response, created_at)
            VALUES (#{idempotencyKey}, #{requestHash}, #{flowId}, CAST(#{responseJson} AS JSONB), #{createdAt})
            ON CONFLICT (idempotency_key) DO NOTHING
            """)
    void insertCommand(CommandRow row);

    @Select("""
            SELECT idempotency_key, request_hash, flow_id, response::text AS response_json, created_at
            FROM apply_commands
            WHERE idempotency_key = #{idempotencyKey}
            """)
    Optional<CommandRow> findCommand(UUID idempotencyKey);

    @Insert("""
            INSERT INTO apply_sources (source_pack_id, version, passages, created_at)
            VALUES (#{sourcePackId}, #{version}, CAST(#{passagesJson} AS JSONB), #{createdAt})
            """)
    void insertSource(
            @Param("sourcePackId") String sourcePackId,
            @Param("version") String version,
            @Param("passagesJson") String passagesJson,
            @Param("createdAt") Instant createdAt
    );

    @Select("""
            SELECT source_pack_id, version, passages::text AS passages_json
            FROM apply_sources
            WHERE source_pack_id = #{sourcePackId}
            """)
    Optional<SourceRow> findSource(String sourcePackId);

    @Insert("""
            INSERT INTO apply_packages (id, attempt_purpose, learner_projection, private_assessor_projection, created_at)
            VALUES (#{id}, #{attemptPurpose}, CAST(#{learnerProjectionJson} AS JSONB),
                    CAST(#{privateAssessorProjectionJson} AS JSONB), #{createdAt})
            """)
    void insertPackage(PackageRow row);

    @Select("""
            SELECT id, attempt_purpose, learner_projection::text AS learner_projection_json,
                   private_assessor_projection::text AS private_assessor_projection_json, created_at
            FROM apply_packages
            WHERE id = #{taskPackageId}
            """)
    Optional<PackageRow> findPackage(UUID taskPackageId);

    @Select("""
            SELECT id, attempt_purpose, learner_projection::text AS learner_projection_json,
                   private_assessor_projection::text AS private_assessor_projection_json, created_at
            FROM apply_packages
            ORDER BY created_at ASC
            """)
    List<PackageRow> listPackages();

    @Insert("""
            INSERT INTO apply_attempts (id, task_package_id, purpose, status, opened_at, closed_at,
                                        submission, assistance_trace)
            VALUES (#{id}, #{taskPackageId}, #{purpose}, #{status}, #{openedAt}, #{closedAt},
                    CAST(#{submissionJson} AS JSONB), CAST(#{assistanceTraceJson} AS JSONB))
            """)
    void insertAttempt(AttemptRow row);

    @Select("""
            SELECT id, task_package_id, purpose, status, opened_at, closed_at,
                   submission::text AS submission_json, assistance_trace::text AS assistance_trace_json
            FROM apply_attempts
            WHERE id = #{attemptId}
            """)
    Optional<AttemptRow> findAttempt(UUID attemptId);

    /**
     * The exposed Task Package ids of one Flow, scoping the open Apply
     * Practice Attempt lookup so a Continue can never resume another Flow's
     * Attempt.
     */
    @Select("""
            SELECT DISTINCT task_package_id
            FROM apply_exposures
            WHERE flow_id = #{flowId}
            """)
    List<UUID> exposedTaskPackageIds(UUID flowId);

    /**
     * The one open Apply Practice Attempt among the given exposed Task
     * Package ids, if any: a PRACTICE-purpose open Attempt (Teach-back
     * packages live in their own table and are excluded). The Workflow Guard
     * uses it as the committed-state fact that a temporary Explain was shown
     * inside an open Attempt.
     */
    @Select("""
            <script>
            SELECT id, task_package_id, purpose, status, opened_at, closed_at,
                   submission::text AS submission_json, assistance_trace::text AS assistance_trace_json
            FROM apply_attempts
            WHERE purpose = 'PRACTICE' AND status = 'OPEN'
              AND task_package_id IN
              <foreach collection='taskPackageIds' item='packageId' open='(' separator=',' close=')'>
                  #{packageId}
              </foreach>
            ORDER BY opened_at ASC
            LIMIT 1
            </script>
            """)
    Optional<AttemptRow> findOpenPracticeAttempt(@Param("taskPackageIds") List<UUID> taskPackageIds);

    @Update("""
            UPDATE apply_attempts
            SET status = 'SUBMITTED', closed_at = #{closedAt}, submission = CAST(#{submissionJson} AS JSONB)
            WHERE id = #{attemptId} AND status = 'OPEN'
            """)
    int closeOpenAttempt(
            @Param("attemptId") UUID attemptId,
            @Param("closedAt") Instant closedAt,
            @Param("submissionJson") String submissionJson
    );

    /**
     * Atomically appends the exposed Assistance Trace and — for the H5 reveal
     * — closes the attempt as Solution Revealed; the open-status guard makes
     * a duplicate or racing exposure a no-op.
     */
    @Update("""
            UPDATE apply_attempts
            SET assistance_trace = CAST(#{assistanceTraceJson} AS JSONB),
                status = #{status},
                closed_at = COALESCE(#{closedAt}, closed_at)
            WHERE id = #{attemptId} AND status = 'OPEN'
            """)
    int appendAssistanceAndReveal(
            @Param("attemptId") UUID attemptId,
            @Param("assistanceTraceJson") String assistanceTraceJson,
            @Param("status") String status,
            @Param("closedAt") Instant closedAt
    );

    /**
     * Appends recorded clarification or temporary-Explain assistance to an
     * OPEN attempt; the open-status guard makes a stale or racing append a
     * no-op.
     */
    @Update("""
            UPDATE apply_attempts
            SET assistance_trace = CAST(#{assistanceTraceJson} AS JSONB)
            WHERE id = #{attemptId} AND status = 'OPEN'
            """)
    int appendAttemptAssistance(
            @Param("attemptId") UUID attemptId,
            @Param("assistanceTraceJson") String assistanceTraceJson
    );

    /**
     * One-way conversion of one open Independent or Review attempt to
     * Practice, appending the recorded assistance: the conditional purpose
     * and open-status guards make a replay or a racing conversion a no-op, so
     * the attempt is converted exactly once and its trace never duplicates.
     */
    @Update("""
            UPDATE apply_attempts
            SET purpose = 'PRACTICE', assistance_trace = CAST(#{assistanceTraceJson} AS JSONB)
            WHERE id = #{attemptId} AND status = 'OPEN'
              AND purpose IN ('INDEPENDENT_TEST', 'REVIEW')
            """)
    int convertAttemptToPractice(
            @Param("attemptId") UUID attemptId,
            @Param("assistanceTraceJson") String assistanceTraceJson
    );

    @Insert("""
            INSERT INTO apply_hint_ladders (attempt_id, ladder, created_at)
            VALUES (#{attemptId}, CAST(#{ladderJson} AS JSONB), #{createdAt})
            ON CONFLICT (attempt_id) DO NOTHING
            """)
    void insertHintLadder(
            @Param("attemptId") UUID attemptId,
            @Param("ladderJson") String ladderJson,
            @Param("createdAt") Instant createdAt
    );

    @Select("""
            SELECT attempt_id, ladder::text AS ladder_json
            FROM apply_hint_ladders
            WHERE attempt_id = #{attemptId}
            """)
    Optional<HintLadderRow> findHintLadder(UUID attemptId);

    @Insert("""
            INSERT INTO apply_hint_requests (
                attempt_id, command_key, requested_level, exposed_level, exposed_at
            ) VALUES (
                #{attemptId}, #{commandKey}, #{requestedLevel}, #{exposedLevel}, #{exposedAt}
            )
            ON CONFLICT (attempt_id, command_key) DO NOTHING
            """)
    void insertHintRequest(HintRequestRow row);

    @Select("""
            SELECT attempt_id, command_key, requested_level, exposed_level, exposed_at
            FROM apply_hint_requests
            WHERE attempt_id = #{attemptId} AND command_key = #{commandKey}
            """)
    Optional<HintRequestRow> findHintRequest(
            @Param("attemptId") UUID attemptId,
            @Param("commandKey") UUID commandKey);

    @Insert("""
            INSERT INTO apply_verifications (id, task_package_id, verdict, created_at)
            VALUES (#{id}, #{taskPackageId}, CAST(#{verdictJson} AS JSONB), #{createdAt})
            """)
    void insertVerification(
            @Param("id") UUID id,
            @Param("taskPackageId") UUID taskPackageId,
            @Param("verdictJson") String verdictJson,
            @Param("createdAt") Instant createdAt
    );

    @Select("""
            SELECT verdict::text AS verdict_json
            FROM apply_verifications
            WHERE task_package_id = #{taskPackageId}
            ORDER BY created_at ASC
            """)
    List<String> listVerificationJson(UUID taskPackageId);

    @Insert("""
            INSERT INTO apply_assessments (id, attempt_id, assessment, created_at)
            VALUES (#{id}, #{attemptId}, CAST(#{assessmentJson} AS JSONB), #{createdAt})
            """)
    void insertAssessment(
            @Param("id") UUID id,
            @Param("attemptId") UUID attemptId,
            @Param("assessmentJson") String assessmentJson,
            @Param("createdAt") Instant createdAt
    );

    @Select("""
            SELECT assessment::text AS assessment_json
            FROM apply_assessments
            WHERE attempt_id = #{attemptId}
            ORDER BY created_at ASC
            """)
    List<String> listAssessmentJson(UUID attemptId);

    @Insert("""
            INSERT INTO apply_evidence (
                id, task_attempt_id, flow_id, concept_id, learner_id, result,
                attempt_purpose, highest_hint_level, assistance_trace, accepted_at
            ) VALUES (
                #{id}, #{taskAttemptId}, #{flowId}, #{conceptId}, #{learnerId}, #{result},
                #{attemptPurpose}, #{highestHintLevel}, CAST(#{assistanceTraceJson} AS JSONB), #{acceptedAt}
            )
            ON CONFLICT (task_attempt_id) DO NOTHING
            """)
    void insertEvidence(EvidenceRow row);

    @Select("SELECT 1 FROM apply_evidence WHERE task_attempt_id = #{attemptId}")
    Optional<Integer> evidenceExists(UUID attemptId);

    @Select("""
            SELECT id, task_attempt_id, flow_id, concept_id, learner_id, result,
                   attempt_purpose, highest_hint_level, assistance_trace::text AS assistance_trace_json,
                   accepted_at
            FROM apply_evidence
            ORDER BY accepted_at ASC, id ASC
            """)
    List<EvidenceRow> listEvidence();

    @Insert("""
            INSERT INTO review_tasks (
                id, learner_id, concept_id, flow_id, review_number, status, due_at,
                created_at, started_at, open_attempt_id, completed_at, cancelled_at
            ) VALUES (
                #{id}, #{learnerId}, #{conceptId}, #{flowId}, #{reviewNumber}, #{status}, #{dueAt},
                #{createdAt}, #{startedAt}, #{openAttemptId}, #{completedAt}, #{cancelledAt}
            )
            """)
    void insertReviewTask(ReviewTaskRow row);

    @Update("""
            UPDATE review_tasks
            SET status = 'CANCELLED', cancelled_at = #{cancelledAt}, open_attempt_id = NULL
            WHERE learner_id = #{learnerId} AND concept_id = #{conceptId}
              AND status IN ('SCHEDULED', 'DUE', 'STARTED')
            """)
    int cancelUnfinishedReviews(
            @Param("learnerId") UUID learnerId,
            @Param("conceptId") UUID conceptId,
            @Param("cancelledAt") Instant cancelledAt
    );

    @Select("""
            SELECT id, learner_id, concept_id, flow_id, review_number, status, due_at,
                   created_at, started_at, open_attempt_id, completed_at, cancelled_at
            FROM review_tasks
            WHERE learner_id = #{learnerId} AND status IN ('SCHEDULED', 'DUE', 'STARTED')
            ORDER BY due_at ASC
            """)
    List<ReviewTaskRow> listUnfinishedReviews(UUID learnerId);

    @Update("""
            UPDATE review_tasks
            SET status = 'DUE'
            WHERE status = 'SCHEDULED' AND due_at <= #{now}
            """)
    int markDueReviewsDue(@Param("now") Instant now);

    @Select("""
            SELECT id, learner_id, concept_id, flow_id, review_number, status, due_at,
                   created_at, started_at, open_attempt_id, completed_at, cancelled_at
            FROM review_tasks
            WHERE id = #{reviewId}
            """)
    Optional<ReviewTaskRow> findReviewTask(UUID reviewId);

    @Select("""
            SELECT id, learner_id, concept_id, flow_id, review_number, status, due_at,
                   created_at, started_at, open_attempt_id, completed_at, cancelled_at
            FROM review_tasks
            WHERE learner_id = #{learnerId} AND concept_id = #{conceptId} AND status = 'STARTED'
            LIMIT 1
            """)
    Optional<ReviewTaskRow> findStartedReview(
            @Param("learnerId") UUID learnerId,
            @Param("conceptId") UUID conceptId);

    /**
     * Atomically cancels the STARTED Review of one learner and Concept after
     * an accepted assistance conversion of its open Attempt. The conditional
     * STARTED guard makes a replay or a racing conversion a no-op; no
     * Evidence is accepted and no milestone changes.
     */
    @Update("""
            UPDATE review_tasks
            SET status = 'CANCELLED', cancelled_at = #{cancelledAt}, open_attempt_id = NULL
            WHERE learner_id = #{learnerId} AND concept_id = #{conceptId} AND status = 'STARTED'
            """)
    int cancelStartedReview(
            @Param("learnerId") UUID learnerId,
            @Param("conceptId") UUID conceptId,
            @Param("cancelledAt") Instant cancelledAt
    );

    @Update("""
            UPDATE review_tasks
            SET status = 'COMPLETED', completed_at = #{completedAt}, open_attempt_id = NULL
            WHERE id = #{reviewId} AND status = 'STARTED'
            """)
    int completeStartedReview(
            @Param("reviewId") UUID reviewId,
            @Param("completedAt") Instant completedAt);

    /**
     * Claims a Review for one new Review Attempt: a Due Review becomes Started,
     * or a Started Review holding no open Attempt is resumed. The claim carries
     * the new Attempt id so the at-most-one-OPEN-Attempt invariant is enforced
     * by the conditional update itself.
     */
    @Update("""
            UPDATE review_tasks
            SET status = 'STARTED',
                started_at = COALESCE(started_at, #{startedAt}),
                open_attempt_id = #{attemptId}
            WHERE id = #{reviewId}
              AND (status = 'DUE' OR (status = 'STARTED' AND open_attempt_id IS NULL))
            """)
    int claimReviewAttempt(
            @Param("reviewId") UUID reviewId,
            @Param("startedAt") Instant startedAt,
            @Param("attemptId") UUID attemptId);

    /**
     * Atomically advances an Inconclusive submission: the Started Review whose
     * open Attempt is the closed submission switches its pointer to the new
     * replacement Attempt, or clears it (null) when no replacement could be
     * prepared, leaving the Review resumable.
     */
    @Update("""
            UPDATE review_tasks
            SET open_attempt_id = #{newOpenAttemptId}
            WHERE id = #{reviewId} AND status = 'STARTED' AND open_attempt_id = #{closedAttemptId}
            """)
    int resolveInconclusiveClaim(
            @Param("reviewId") UUID reviewId,
            @Param("closedAttemptId") UUID closedAttemptId,
            @Param("newOpenAttemptId") UUID newOpenAttemptId);

    @Insert("""
            INSERT INTO apply_teach_back_anchors (flow_id, anchor_id, anchor_kind, exposed_at)
            VALUES (#{flowId}, #{anchorId}, #{anchorKind}, #{exposedAt})
            ON CONFLICT (flow_id, anchor_id) DO NOTHING
            """)
    void insertTeachBackAnchor(
            @Param("flowId") UUID flowId,
            @Param("anchorId") UUID anchorId,
            @Param("anchorKind") String anchorKind,
            @Param("exposedAt") Instant exposedAt
    );

    @Select("""
            SELECT flow_id, anchor_id, anchor_kind, exposed_at
            FROM apply_teach_back_anchors
            WHERE flow_id = #{flowId}
            ORDER BY exposed_at ASC, anchor_id ASC
            """)
    List<TeachBackAnchorRow> listTeachBackAnchors(UUID flowId);

    @Insert("""
            INSERT INTO apply_teach_back_packages (
                id, attempt_purpose, learner_projection, private_projection, created_at
            ) VALUES (
                #{id}, #{attemptPurpose}, CAST(#{learnerProjectionJson} AS JSONB),
                CAST(#{privateProjectionJson} AS JSONB), #{createdAt}
            )
            """)
    void insertTeachBackPackage(TeachBackPackageRow row);

    @Select("""
            SELECT id, attempt_purpose, learner_projection::text AS learner_projection_json,
                   private_projection::text AS private_projection_json, created_at
            FROM apply_teach_back_packages
            WHERE id = #{taskPackageId}
            """)
    Optional<TeachBackPackageRow> findTeachBackPackage(UUID taskPackageId);

    @Insert("""
            INSERT INTO apply_teach_back_assessments (id, attempt_id, assessment, created_at)
            VALUES (#{id}, #{attemptId}, CAST(#{assessmentJson} AS JSONB), #{createdAt})
            """)
    void insertTeachBackAssessment(
            @Param("id") UUID id,
            @Param("attemptId") UUID attemptId,
            @Param("assessmentJson") String assessmentJson,
            @Param("createdAt") Instant createdAt
    );

    @Select("""
            SELECT assessment::text AS assessment_json
            FROM apply_teach_back_assessments
            WHERE attempt_id = #{attemptId}
            ORDER BY created_at ASC
            """)
    List<String> listTeachBackAssessmentJson(UUID attemptId);

    record ApplyFlowRow(
            UUID id,
            UUID learnerId,
            UUID conceptId,
            String status,
            String stage,
            Instant createdAt
    ) {
    }

    record InteractionRow(
            UUID id,
            UUID flowId,
            int interactionVersion,
            String status,
            String stage,
            UUID attemptId,
            String attemptPurpose,
            String learnerProjectionJson,
            String learnerMessage,
            String teachingProjectionJson,
            String hintJson,
            String assistanceConsentJson,
            Instant createdAt
    ) {
    }

    record CheckpointRow(UUID id, UUID flowId, int interactionVersion, Instant createdAt) {
    }

    record CommandRow(
            UUID idempotencyKey,
            String requestHash,
            UUID flowId,
            String responseJson,
            Instant createdAt
    ) {
    }

    record SourceRow(String sourcePackId, String version, String passagesJson) {
    }

    record PackageRow(
            UUID id,
            String attemptPurpose,
            String learnerProjectionJson,
            String privateAssessorProjectionJson,
            Instant createdAt
    ) {
    }

    record AttemptRow(
            UUID id,
            UUID taskPackageId,
            String purpose,
            String status,
            Instant openedAt,
            Instant closedAt,
            String submissionJson,
            String assistanceTraceJson
    ) {
    }

    record HintLadderRow(UUID attemptId, String ladderJson) {
    }

    record HintRequestRow(
            UUID attemptId,
            UUID commandKey,
            int requestedLevel,
            int exposedLevel,
            Instant exposedAt
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

    record ReviewTaskRow(
            UUID id,
            UUID learnerId,
            UUID conceptId,
            UUID flowId,
            int reviewNumber,
            String status,
            Instant dueAt,
            Instant createdAt,
            Instant startedAt,
            UUID openAttemptId,
            Instant completedAt,
            Instant cancelledAt
    ) {
    }

    record ExplainArtifactRow(String artifactJson) {
    }

    record TeachBackAnchorRow(UUID flowId, UUID anchorId, String anchorKind, Instant exposedAt) {
    }

    record TeachBackPackageRow(
            UUID id,
            String attemptPurpose,
            String learnerProjectionJson,
            String privateProjectionJson,
            Instant createdAt
    ) {
    }
}
