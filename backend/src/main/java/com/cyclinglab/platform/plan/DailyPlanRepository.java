package com.cyclinglab.platform.plan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyPlanRepository extends JpaRepository<DailyPlanEntity, UUID> {

    Optional<DailyPlanEntity> findByIdAndWeeklyPlan_Id(UUID id, UUID weeklyPlanId);

    List<DailyPlanEntity> findAllByWeeklyPlan_IdOrderByPlanDateAsc(UUID weeklyPlanId);
}