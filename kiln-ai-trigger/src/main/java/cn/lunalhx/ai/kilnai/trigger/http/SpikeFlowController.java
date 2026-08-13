package cn.lunalhx.ai.kilnai.trigger.http;

import cn.lunalhx.ai.kilnai.api.dto.SpikeFlowResponse;
import cn.lunalhx.ai.kilnai.api.dto.SpikeTraceResponse;
import cn.lunalhx.ai.kilnai.api.dto.StartSpikeFlowRequest;
import cn.lunalhx.ai.kilnai.api.dto.SubmitSpikeEventRequest;
import cn.lunalhx.ai.kilnai.domain.learning.model.LearnerVisibleInteraction;
import cn.lunalhx.ai.kilnai.domain.learning.model.PublicTraceView;
import cn.lunalhx.ai.kilnai.domain.learning.service.ResumeGraphRun;
import cn.lunalhx.ai.kilnai.domain.learning.service.StartGraphRun;
import cn.lunalhx.ai.kilnai.domain.learning.service.LearningFlowUseCase;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearnerInputKind;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/spike/flows")
public class SpikeFlowController {

    private final LearningFlowUseCase useCase;

    public SpikeFlowController(LearningFlowUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SpikeFlowResponse start(
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody StartSpikeFlowRequest request
    ) {
        return toResponse(useCase.start(new StartGraphRun(
                request.learnerId(), request.fixtureId(), idempotencyKey, null
        )));
    }

    @PostMapping("/{flowId}/events")
    public SpikeFlowResponse event(
            @PathVariable UUID flowId,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody SubmitSpikeEventRequest request
    ) {
        LearnerInputKind kind = LearnerInputKind.valueOf(request.kind());
        return toResponse(useCase.resume(new ResumeGraphRun(
                flowId, idempotencyKey, request.interactionVersion(), kind, request.text()
        )));
    }

    @GetMapping("/{flowId}")
    public SpikeFlowResponse get(@PathVariable UUID flowId) {
        return toResponse(useCase.query(flowId));
    }

    @GetMapping("/{flowId}/trace")
    public SpikeTraceResponse trace(@PathVariable UUID flowId) {
        PublicTraceView view = useCase.trace(flowId);
        return new SpikeTraceResponse(
                view.flowId(), view.routes(), view.selectedSkills(), view.checkpoints(),
                view.budget(), view.validations(), view.retries()
        );
    }

    private SpikeFlowResponse toResponse(LearnerVisibleInteraction interaction) {
        return new SpikeFlowResponse(
                interaction.flowId(),
                interaction.status().name(),
                interaction.stage().name(),
                interaction.interactionVersion(),
                interaction.visibleContent(),
                interaction.allowedEventKinds().stream().map(Enum::name).toList()
        );
    }
}
