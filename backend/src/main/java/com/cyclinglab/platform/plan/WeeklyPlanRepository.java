package com.cyclinglab.platform.plan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WeeklyPlanRepository extends JpaRepository<WeeklyPlanEntity, UUID> {

    Optional<WeeklyPlanEntity> findByIdAndUser_Id(UUID id, UUID userId);

    Optional<WeeklyPlanEntity> findByUser_IdAndIsoYearAndIsoWeek(UUID userId, Short isoYear, Short isoWeek);

    boolean existsByUser_IdAndIsoYearAndIsoWeek(UUID userId, Short isoYear, Short isoWeek);

    @Query("select w from WeeklyPlanEntity w where w.user.id = :userId order by w.isoYear desc, w.isoWeek desc")
    Page<WeeklyPlanEntity> findAllByUser(@Param("userId") UUID userId, Pageable pageable);

    @Query("select w from WeeklyPlanEntity w where w.user.id = :userId and w.isoYear = :year order by w.isoWeek desc")
    List<WeeklyPlanEntity> findAllByUserAndYear(@Param("userId") UUID userId, @Param("year") Short year);
}
