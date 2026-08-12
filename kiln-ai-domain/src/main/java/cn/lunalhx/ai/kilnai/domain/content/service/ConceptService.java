package cn.lunalhx.ai.kilnai.domain.content.service;

import cn.lunalhx.ai.kilnai.domain.content.adapter.port.ConceptRepository;
import cn.lunalhx.ai.kilnai.domain.content.model.entity.Concept;

import java.time.Clock;
import java.util.UUID;

public class ConceptService {

    private final ConceptRepository conceptRepository;
    private final Clock clock;

    public ConceptService(ConceptRepository conceptRepository, Clock clock) {
        this.conceptRepository = conceptRepository;
        this.clock = clock;
    }

    public Concept create(String title, String summary, String sourceReference) {
        return conceptRepository.save(new Concept(
                UUID.randomUUID(), title, summary, sourceReference, clock.instant()
        ));
    }
}
