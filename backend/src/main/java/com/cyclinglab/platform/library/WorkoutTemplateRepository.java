package com.cyclinglab.platform.library;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkoutTemplateRepository extends JpaRepository<WorkoutTemplateEntity, UUID> {

    Page<WorkoutTemplateEntity> findAllByIsArchivedFalse(Pageable pageable);

    Page<WorkoutTemplateEntity> findAllByIsArchived(boolean archived, Pageable pageable);

    Page<WorkoutTemplateEntity> findAllByCategoryAndIsArchivedFalse(
        WorkoutCategory category, Pageable pageable);

    /** Owner-scoped lookup. Returns the template only when it belongs to
     *  the given user. Used as the authoritative multi-tenant check; the
     *  Hibernate {@code tenantFilter} is a defense-in-depth on top. */
    Optional<WorkoutTemplateEntity> findByIdAndUser_Id(UUID id, UUID userId);

    /** Owner-scoped lookup, including archived templates (used by archive
     *  confirmation and the duplicate path which may want to copy an
     *  archived one). */
    Optional<WorkoutTemplateEntity> findByIdAndUser_IdAndIsArchivedFalse(UUID id, UUID userId);

    boolean existsByUser_IdAndNameAndIsArchivedFalse(UUID userId, String name);

    boolean existsByUser_IdAndName(UUID userId, String name);

    List<WorkoutTemplateEntity> findAllByUser_IdAndIsArchivedFalse(UUID userId);
}
