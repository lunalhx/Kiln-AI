package cn.lunalhx.ai.kilnai.trigger.http;

import cn.lunalhx.ai.kilnai.api.dto.ConceptResponse;
import cn.lunalhx.ai.kilnai.api.dto.CreateConceptRequest;
import cn.lunalhx.ai.kilnai.domain.content.model.entity.Concept;
import cn.lunalhx.ai.kilnai.domain.content.service.ConceptService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/concepts")
public class ConceptController {

    private final ConceptService conceptService;

    public ConceptController(ConceptService conceptService) {
        this.conceptService = conceptService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConceptResponse create(@Valid @RequestBody CreateConceptRequest request) {
        Concept concept = conceptService.create(request.title(), request.summary(), request.sourceReference());
        return new ConceptResponse(
                concept.id(), concept.title(), concept.summary(), concept.sourceReference(), concept.createdAt()
        );
    }
}
