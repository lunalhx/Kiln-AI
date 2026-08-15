package cn.lunalhx.ai.kilnai.trigger.http;

import cn.lunalhx.ai.kilnai.api.dto.ApplyFlowResponse;
import cn.lunalhx.ai.kilnai.api.dto.StartApplyFlowRequest;
import cn.lunalhx.ai.kilnai.api.dto.SubmitApplyFlowRequest;
import cn.lunalhx.ai.kilnai.api.response.ApiErrorResponse;
import cn.lunalhx.ai.kilnai.domain.apply.flow.ApplyFlowUseCase;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyFlowResult;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * The learner-facing Apply flow API: start, query, and one formal submission
 * per displayed task. It is idempotent per {@code Idempotency-Key} and
 * rejects stale interaction versions with 409; a rejected submission returns
 * 422. Public responses carry only the safe Concept Progress projection and
 * never private assessor projections.
 */
@RestController
@RequestMapping("/api/apply/flows")
public class ApplyFlowController {

    private final ApplyFlowUseCase useCase;
    private final ApplyFlowResponseMapper responseMapper;

    public ApplyFlowController(ApplyFlowUseCase useCase, ApplyFlowResponseMapper responseMapper) {
        this.useCase = Objects.requireNonNull(useCase, "useCase must not be null");
        this.responseMapper = Objects.requireNonNull(responseMapper, "responseMapper must not be null");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplyFlowResponse start(
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody StartApplyFlowRequest request
    ) {
        ApplyFlowResult result = useCase.start(request.learnerId(), idempotencyKey);
        return responseMapper.toResponse(((ApplyFlowResult.Boundary) result).interaction());
    }

    @PostMapping("/{flowId}/submissions")
    public ResponseEntity<?> submit(
            @PathVariable UUID flowId,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody SubmitApplyFlowRequest request
    ) {
        ApplyFlowResult result = useCase.submit(
                flowId, request.interactionVersion(), idempotencyKey, request.attemptId(),
                request.rawDerivative(), request.confirmedCanonical(), request.rationale());
        return switch (result) {
            case ApplyFlowResult.Boundary boundary ->
                    ResponseEntity.ok(responseMapper.toResponse(boundary.interaction()));
            case ApplyFlowResult.SubmissionRejected rejected -> ResponseEntity
                    .status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(new ApiErrorResponse(ErrorCode.UNPROCESSABLE.name(),
                            rejected.reason().name(), Instant.now()));
            case ApplyFlowResult.SubmissionIgnored ignored -> ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new ApiErrorResponse(ErrorCode.CONFLICT.name(),
                            ignored.reason().name(), Instant.now()));
        };
    }

    @GetMapping("/{flowId}")
    public ApplyFlowResponse get(@PathVariable UUID flowId) {
        return responseMapper.toResponse(useCase.query(flowId));
    }
}
