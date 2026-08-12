package cn.lunalhx.ai.kilnai.trigger.http;

import cn.lunalhx.ai.kilnai.api.dto.LearningProgressResponse;
import cn.lunalhx.ai.kilnai.api.dto.RecordLearningEvidenceRequest;
import cn.lunalhx.ai.kilnai.domain.learning.service.LearningProgressResult;
import cn.lunalhx.ai.kilnai.domain.learning.service.RecordLearningEvidenceCommand;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningEventType;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningResult;
import cn.lunalhx.ai.kilnai.trigger.application.LearningCommandService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/concepts/{conceptId}/learning-events")
public class LearningController {

    private final LearningCommandService learningCommandService;

    public LearningController(LearningCommandService learningCommandService) {
        this.learningCommandService = learningCommandService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LearningProgressResponse record(
            @PathVariable UUID conceptId,
            @Valid @RequestBody RecordLearningEvidenceRequest request
    ) {
        LearningProgressResult result = learningCommandService.recordEvidence(
                new RecordLearningEvidenceCommand(
                        request.userId(), conceptId, LearningEventType.valueOf(request.eventType()),
                        LearningResult.valueOf(request.result()), request.hintLevel(),
                        request.delayedReview(), request.transfer(), request.occurredAt(), request.confidence(),
                        request.errorTag()
                )
        );
        return new LearningProgressResponse(
                result.userId(), result.conceptId(), result.state().name(), result.nextAction().name(),
                result.nextReviewDueAt()
        );
    }
}
