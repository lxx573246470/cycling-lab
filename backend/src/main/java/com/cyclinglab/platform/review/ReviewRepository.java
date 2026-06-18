package com.cyclinglab.platform.review;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<ReviewEntity, UUID> {

    Optional<ReviewEntity> findByIdAndUser_Id(UUID id, UUID userId);

    Optional<ReviewEntity> findByUser_IdAndScopeAndIsoYearAndIsoWeek(
        UUID userId, ReviewScope scope, Short isoYear, Short isoWeek
    );

    Page<ReviewEntity> findAllByUser_IdOrderByUpdatedAtDesc(UUID userId, Pageable pageable);

    List<ReviewEntity> findAllByUser_IdAndScopeOrderByUpdatedAtDesc(UUID userId, ReviewScope scope);
}
