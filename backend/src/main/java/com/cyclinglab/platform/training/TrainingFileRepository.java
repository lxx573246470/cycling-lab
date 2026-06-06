package com.cyclinglab.platform.training;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrainingFileRepository extends JpaRepository<TrainingFileEntity, UUID> {

    Optional<TrainingFileEntity> findByIdAndUser_Id(UUID id, UUID userId);

    Page<TrainingFileEntity> findAllByUser_Id(UUID userId, Pageable pageable);

    boolean existsByUser_IdAndSha256(UUID userId, String sha256);
}
