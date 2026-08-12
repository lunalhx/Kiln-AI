package cn.lunalhx.ai.kilnai.domain.content.adapter.port;

import cn.lunalhx.ai.kilnai.domain.content.model.entity.Concept;

import java.util.Optional;
import java.util.UUID;

/** Output port owned by the domain. Persistence adapters implement this contract. */
public interface ConceptRepository {

    Concept save(Concept concept);

    Optional<Concept> findById(UUID id);
}
