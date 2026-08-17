package cn.lunalhx.ai.kilnai.trigger.http;

import cn.lunalhx.ai.kilnai.api.dto.LearningFlowCommandRequest;
import cn.lunalhx.ai.kilnai.api.dto.LearningFlowResponse;
import cn.lunalhx.ai.kilnai.api.dto.StartLearningFlowRequest;
import cn.lunalhx.ai.kilnai.api.response.ApiErrorResponse;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyFlowResult;
import cn.lunalhx.ai.kilnai.domain.learning.graph.LearningFlowCommandUseCase;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
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
import java.util.Set;
import java.util.UUID;

/**
 * The unified learner-facing Learning Flow API: start a Flow, query its
 * latest committed interaction, and issue one closed discriminated command.
 * Every command is idempotent per {@code Idempotency-Key} and rejects a stale
 * {@code interactionVersion} with 409; a command that was never legal for the
 * addressed Attempt is ignored with 409, and a rejected submission returns
 * 422. Public responses carry only the closed committed-interaction union and
 * never private assessor projections, expected answers, source passages, or
 * execution traces.
 */
@RestController
@RequestMapping("/api/learning/flows")
public class LearningFlowController {

    private final LearningFlowCommandUseCase useCase;
    private final LearningFlowResponseMapper responseMapper;

    private static final Set<String> ATTEMPT_COMMANDS =
            Set.of("answer_submitted", "hint_requested", "clarification_asked", "assistance_decided");

    public LearningFlowController(
            LearningFlowCommandUseCase useCase,
            LearningFlowResponseMapper responseMapper
    ) {
        this.useCase = Objects.requireNonNull(useCase, "useCase must not be null");
        this.responseMapper = Objects.requireNonNull(responseMapper, "responseMapper must not be null");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LearningFlowResponse start(
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody StartLearningFlowRequest request
    ) {
        ApplyFlowResult result = useCase.start(request.learnerId(), idempotencyKey);
        return responseMapper.toResponse(((ApplyFlowResult.Boundary) result).interaction());
    }

    @GetMapping("/{flowId}")
    public LearningFlowResponse get(@PathVariable UUID flowId) {
        return responseMapper.toResponse(useCase.query(flowId));
    }

    @PostMapping("/{flowId}/commands")
    public ResponseEntity<?> command(
            @PathVariable UUID flowId,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody LearningFlowCommandRequest request
    ) {
        int interactionVersion = requireVersion(request);
        ApplyFlowResult result = switch (request.command()) {
            case "answer_submitted" -> useCase.submitAnswer(
                    flowId, interactionVersion, idempotencyKey, requireAttempt(request),
                    requireRawAnswer(request), request.confirmedCanonical(), request.rationale());
            case "hint_requested" -> useCase.requestHint(
                    flowId, interactionVersion, requireAttempt(request),
                    Boolean.TRUE.equals(request.answerRequested()), idempotencyKey);
            case "clarification_asked" -> useCase.clarificationAsked(
                    flowId, interactionVersion, requireAttempt(request), requireMessage(request), idempotencyKey);
            case "assistance_decided" -> useCase.assistanceDecided(
                    flowId, interactionVersion, requireAttempt(request),
                    Boolean.TRUE.equals(request.accept()), idempotencyKey);
            case "continue_requested" -> useCase.continueRequested(
                    flowId, interactionVersion, idempotencyKey);
            case "flow_control_requested" -> useCase.flowControlRequested(
                    flowId, interactionVersion, idempotencyKey);
            default -> throw new ApplicationException(
                    ErrorCode.INVALID_ARGUMENT, "unknown command: " + request.command());
        };
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
            case ApplyFlowResult.HintIgnored ignored -> ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new ApiErrorResponse(ErrorCode.CONFLICT.name(),
                            ignored.reason().name(), Instant.now()));
            case ApplyFlowResult.ClarificationIgnored ignored -> ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new ApiErrorResponse(ErrorCode.CONFLICT.name(),
                            ignored.reason().name(), Instant.now()));
            case ApplyFlowResult.AssistanceIgnored ignored -> ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new ApiErrorResponse(ErrorCode.CONFLICT.name(),
                            ignored.reason().name(), Instant.now()));
        };
    }

    private int requireVersion(LearningFlowCommandRequest request) {
        if (request.interactionVersion() == null) {
            throw new ApplicationException(ErrorCode.INVALID_ARGUMENT, "interactionVersion is required");
        }
        return request.interactionVersion();
    }

    private UUID requireAttempt(LearningFlowCommandRequest request) {
        if (request.attemptId() == null) {
            throw new ApplicationException(
                    ErrorCode.INVALID_ARGUMENT, "attemptId is required for " + request.command());
        }
        return request.attemptId();
    }

    private String requireRawAnswer(LearningFlowCommandRequest request) {
        if (request.rawAnswer() == null || request.rawAnswer().isBlank()) {
            throw new ApplicationException(
                    ErrorCode.INVALID_ARGUMENT, "rawAnswer is required for answer_submitted");
        }
        return request.rawAnswer();
    }

    private String requireMessage(LearningFlowCommandRequest request) {
        if (request.message() == null || request.message().isBlank()) {
            throw new ApplicationException(
                    ErrorCode.INVALID_ARGUMENT, "message is required for clarification_asked");
        }
        return request.message();
    }
}
