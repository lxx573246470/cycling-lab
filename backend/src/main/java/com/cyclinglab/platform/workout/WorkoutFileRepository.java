package com.cyclinglab.platform.workout;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkoutFileRepository extends JpaRepository<WorkoutFileEntity, UUID> {

    Optional<WorkoutFileEntity> findByIdAndUser_Id(UUID id, UUID userId);

    Page<WorkoutFileEntity> findAllByUser_Id(UUID userId, Pageable pageable);

    long countBySourceTemplateId(UUID sourceTemplateId);
}