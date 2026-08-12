package cn.lunalhx.ai.kilnai.domain.learning.service;

import cn.lunalhx.ai.kilnai.domain.content.adapter.port.ConceptRepository;
import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.LearnerConceptProgressRepository;
import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.LearningEventRepository;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.LearnerConceptProgress;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.LearningEvent;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.ConceptState;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningEvidence;
import cn.lunalhx.ai.kilnai.domain.pedagogy.model.valobj.TeachingAction;
import cn.lunalhx.ai.kilnai.domain.pedagogy.service.LearningWorkflow;
import cn.lunalhx.ai.kilnai.domain.review.adapter.port.ReviewTaskRepository;
import cn.lunalhx.ai.kilnai.domain.review.model.entity.ReviewTask;
import cn.lunalhx.ai.kilnai.domain.review.model.valobj.ReviewTaskStatus;
import cn.lunalhx.ai.kilnai.domain.review.model.valobj.ReviewTaskType;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** Coordinates the learning aggregate and its output ports for a single assessed event. */
public class LearningProgressService {

    private static final Duration FIRST_REVIEW_DELAY = Duration.ofDays(1);

    private final ConceptRepository conceptRepository;
    private final LearnerConceptProgressRepository progressRepository;
    private final LearningEventRepository learningEventRepository;
    private final ReviewTaskRepository reviewTaskRepository;
    private final LearningWorkflow learningWorkflow;
    private final Clock clock;

    public LearningProgressService(
            ConceptRepository conceptRepository,
            LearnerConceptProgressRepository progressRepository,
            LearningEventRepository learningEventRepository,
            ReviewTaskRepository reviewTaskRepository,
            LearningWorkflow learningWorkflow,
            Clock clock
    ) {
        this.conceptRepository = conceptRepository;
        this.progressRepository = progressRepository;
        this.learningEventRepository = learningEventRepository;
        this.reviewTaskRepository = reviewTaskRepository;
        this.learningWorkflow = learningWorkflow;
        this.clock = clock;
    }

    public LearningProgressResult recordEvidence(RecordLearningEvidenceCommand command) {
        conceptRepository.findById(command.conceptId()).orElseThrow(() -> new ApplicationException(
                ErrorCode.CONCEPT_NOT_FOUND, "Concept not found: " + command.conceptId()
        ));

        Instant recordedAt = clock.instant();
        LearningEvidence evidence = new LearningEvidence(
                command.eventType(), command.result(), command.hintLevel(), command.delayedReview(),
                command.transfer(), command.occurredAt()
        );
        LearnerConceptProgress progress = progressRepository
                .findByUserIdAndConceptId(command.userId(), command.conceptId())
                .orElseGet(() -> LearnerConceptProgress.start(command.userId(), command.conceptId(), recordedAt));

        ConceptState previousState = progress.state();
        ConceptState currentState = progress.record(evidence);
        learningEventRepository.append(new LearningEvent(
                UUID.randomUUID(), command.userId(), command.conceptId(), evidence,
                command.confidence(), command.errorTag(), recordedAt
        ));
        progressRepository.save(progress);

        Instant nextReviewDueAt = null;
        if (previousState != ConceptState.INDEPENDENT && currentState == ConceptState.INDEPENDENT) {
            nextReviewDueAt = recordedAt.plus(FIRST_REVIEW_DELAY);
            reviewTaskRepository.save(new ReviewTask(
                    UUID.randomUUID(), command.userId(), command.conceptId(), ReviewTaskType.RETRIEVE,
                    ReviewTaskStatus.PENDING, nextReviewDueAt, recordedAt
            ));
        }

        TeachingAction nextAction = learningWorkflow.nextAction(currentState, evidence);
        return new LearningProgressResult(
                command.userId(), command.conceptId(), currentState, nextAction, nextReviewDueAt
        );
    }
}
