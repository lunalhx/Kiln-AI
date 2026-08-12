package cn.lunalhx.ai.kilnai.infrastructure.adapter.repository;

import cn.lunalhx.ai.kilnai.domain.content.model.entity.Concept;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;
import java.util.UUID;

@Mapper
public interface ConceptMapper {

    @Insert("""
            INSERT INTO concepts (id, title, summary, source_reference, created_at)
            VALUES (#{id}, #{title}, #{summary}, #{sourceReference}, #{createdAt})
            """)
    void insert(Concept concept);

    @Select("""
            SELECT id, title, summary, source_reference, created_at
            FROM concepts
            WHERE id = #{id}
            """)
    Optional<Concept> findById(UUID id);
}
