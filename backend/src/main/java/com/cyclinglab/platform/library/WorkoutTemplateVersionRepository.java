package com.cyclinglab.platform.library;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkoutTemplateVersionRepository
    extends JpaRepository<WorkoutTemplateVersionEntity, UUID> {

    List<WorkoutTemplateVersionEntity> findAllByTemplateIdOrderByVersionDesc(UUID templateId);

    Optional<WorkoutTemplateVersionEntity> findByTemplateIdAndVersion(UUID templateId, Integer version);

    long countByTemplateId(UUID templateId);

    boolean existsByTemplateIdAndVersion(UUID templateId, Integer version);
}