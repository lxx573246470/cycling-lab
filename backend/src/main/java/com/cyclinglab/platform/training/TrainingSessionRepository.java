package com.cyclinglab.platform.training;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrainingSessionRepository extends JpaRepository<TrainingSessionEntity, UUID> {

    Optional<TrainingSessionEntity> findByIdAndUser_Id(UUID id, UUID userId);

    Optional<TrainingSessionEntity> findByFile_Id(UUID fileId);
}
