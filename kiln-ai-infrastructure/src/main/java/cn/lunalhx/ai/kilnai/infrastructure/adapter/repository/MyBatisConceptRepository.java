package cn.lunalhx.ai.kilnai.infrastructure.adapter.repository;

import cn.lunalhx.ai.kilnai.domain.content.adapter.port.ConceptRepository;
import cn.lunalhx.ai.kilnai.domain.content.model.entity.Concept;

import java.util.Optional;
import java.util.UUID;

public class MyBatisConceptRepository implements ConceptRepository {

    private final ConceptMapper mapper;

    public MyBatisConceptRepository(ConceptMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Concept save(Concept concept) {
        mapper.insert(concept);
        return concept;
    }

    @Override
    public Optional<Concept> findById(UUID id) {
        return mapper.findById(id);
    }
}
