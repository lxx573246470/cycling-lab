package com.cyclinglab.platform.plan;

import com.cyclinglab.platform.library.WorkoutTemplateEntity;
import com.cyclinglab.platform.library.WorkoutTemplateRepository;
import com.cyclinglab.platform.library.WorkoutTemplateVersionRepository;
import com.cyclinglab.platform.library.dto.PageResponse;
import com.cyclinglab.platform.library.exception.TemplateNotFoundException;
import com.cyclinglab.platform.plan.dto.DailyPlanDto;
import com.cyclinglab.platform.plan.dto.DailyPlanUpdateRequest;
import com.cyclinglab.platform.plan.dto.WeeklyPlanCreateRequest;
import com.cyclinglab.platform.plan.dto.WeeklyPlanDto;
import com.cyclinglab.platform.plan.dto.WeeklyPlanDto.WeeklyPlanProgress;
import com.cyclinglab.platform.plan.dto.WeeklyPlanSummaryDto;
import com.cyclinglab.platform.plan.dto.WeeklyPlanUpdateRequest;
import com.cyclinglab.platform.plan.exception.DailyPlanNotFoundException;
import com.cyclinglab.platform.plan.exception.WeeklyPlanConflictException;
import com.cyclinglab.platform.plan.exception.WeeklyPlanNotFoundException;
import com.cyclinglab.platform.tenant.TenantContext;
import com.cyclinglab.platform.user.UserEntity;
import com.cyclinglab.platform.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Weekly plan service. Tenant isolation via the {@code tenantFilter} Hibernate
 * filter plus owner-scoped repository lookups (same belt-and-suspenders
 * pattern as {@code LibraryService}). All cross-module references (template
 * ownership) are validated here so a controller can never accidentally
 * overwrite another user's plan.
 */
@Service
@RequiredArgsConstructor
public class WeeklyPlanService {

    private final WeeklyPlanRepository weekRepo;
    private final DailyPlanRepository dayRepo;
    private final WorkoutTemplateRepository templateRepo;
    private final WorkoutTemplateVersionRepository versionRepo;
    private final UserRepository userRepo;

    @Transactional(readOnly = true)
    public PageResponse<WeeklyPlanSummaryDto> list(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        UUID userId = TenantContext.getCurrentUserId();
        Pageable pageable = PageRequest.of(
            safePage, safeSize,
            Sort.by(Sort.Direction.DESC, "isoYear", "isoWeek")
        );
        Page<WeeklyPlanEntity> p = weekRepo.findAllByUser(userId, pageable);
        List<WeeklyPlanSummaryDto> items = p.getContent().stream()
            .map(this::toSummary)
            .toList();
        return new PageResponse<>(
            items, safePage, safeSize, p.getTotalElements(), p.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public WeeklyPlanDto get(UUID id) {
        WeeklyPlanEntity e = loadOwned(id);
        return toDetail(e, resolveTemplateNames(e));
    }

    @Transactional
    public WeeklyPlanDto create(WeeklyPlanCreateRequest req) {
        UUID userId = TenantContext.getCurrentUserId();
        // De-dup check up front so the database unique index never has to fire
        // a constraint violation; we still rely on the index for race-safety.
        weekRepo.findByUser_IdAndIsoYearAndIsoWeek(userId, req.isoYear(), req.isoWeek())
            .ifPresent(existing -> {
                throw new WeeklyPlanConflictException(
                    req.isoYear(), req.isoWeek(), existing.getId()
                );
            });

        UserEntity user = userRepo.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Current user not found"));

        List<LocalDate> dates = IsoWeek.datesOf(req.isoYear(), req.isoWeek());

        WeeklyPlanEntity e = new WeeklyPlanEntity();
        e.setId(UUID.randomUUID());
        e.setUser(user);
        e.setIsoYear(req.isoYear());
        e.setIsoWeek(req.isoWeek());
        e.setTitle(emptyToNull(req.title()));
        e.setGoalMd(emptyToNull(req.goalMd()));

        for (LocalDate d : dates) {
            DailyPlanEntity day = new DailyPlanEntity();
            day.setId(UUID.randomUUID());
            day.setWeeklyPlan(e);
            day.setPlanDate(d);
            day.setWeekday(IsoWeek.weekday(d));
            day.setStatus(DailyPlanStatus.PLANNED);
            e.getDays().add(day);
        }

        WeeklyPlanEntity saved = weekRepo.save(e);
        // Saved is returned with refreshed days (already in cascade order).
        return toDetail(saved, Map.of());
    }

    @Transactional
    public WeeklyPlanDto update(UUID id, WeeklyPlanUpdateRequest req) {
        WeeklyPlanEntity e = loadOwned(id);
        if (req.title() != null) {
            e.setTitle(emptyToNull(req.title()));
        }
        if (req.goalMd() != null) {
            e.setGoalMd(emptyToNull(req.goalMd()));
        }
        // No save() call needed; JPA dirty-checking flushes on commit.
        return toDetail(e, resolveTemplateNames(e));
    }

    @Transactional
    public void delete(UUID id) {
        WeeklyPlanEntity e = loadOwned(id);
        weekRepo.delete(e);
    }

    @Transactional
    public DailyPlanDto updateDay(UUID weekId, UUID dayId, DailyPlanUpdateRequest req) {
        WeeklyPlanEntity week = loadOwned(weekId);
        DailyPlanEntity day = dayRepo.findByIdAndWeeklyPlan_Id(dayId, week.getId())
            .orElseThrow(() -> new DailyPlanNotFoundException(dayId));

        if (req.targetText() != null) {
            day.setTargetText(emptyToNull(req.targetText()));
        }
        if (Boolean.TRUE.equals(req.templateIdPresent())) {
            // explicit clear
            day.setTemplateId(null);
            day.setTemplateVersion(null);
        } else if (req.templateId() != null) {
            UUID templateId = req.templateId();
            UUID userId = TenantContext.getCurrentUserId();
            WorkoutTemplateEntity tpl = templateRepo
                .findByIdAndUser_IdAndIsArchivedFalse(templateId, userId)
                .or(() -> templateRepo.findByIdAndUser_Id(templateId, userId))
                .orElseThrow(() -> new TemplateNotFoundException(templateId));
            int resolvedVersion = req.templateVersion() != null
                ? req.templateVersion()
                : tpl.getCurrentVersion();
            // guard against pinning a version that does not exist for the
            // template, which would make the day look unresolvable later
            if (!versionRepo.existsByTemplateIdAndVersion(tpl.getId(), resolvedVersion)) {
                throw new IllegalArgumentException(
                    "template version " + resolvedVersion + " does not exist for template " + templateId
                );
            }
            day.setTemplateId(tpl.getId());
            day.setTemplateVersion(resolvedVersion);
        }
        if (req.notesMd() != null) {
            day.setNotesMd(emptyToNull(req.notesMd()));
        }
        if (req.status() != null) {
            day.setStatus(req.status());
        }

        Map<UUID, String> names = resolveTemplateNames(week);
        UUID tid = day.getTemplateId();
        return toDayDto(day, tid == null ? null : names.get(tid));
    }

    // ---- helpers ----------------------------------------------------------

    private WeeklyPlanEntity loadOwned(UUID id) {
        UUID userId = TenantContext.getCurrentUserId();
        return weekRepo.findByIdAndUser_Id(id, userId)
            .orElseThrow(() -> new WeeklyPlanNotFoundException(id));
    }

    private Map<UUID, String> resolveTemplateNames(WeeklyPlanEntity e) {
        Set<UUID> ids = e.getDays().stream()
            .map(DailyPlanEntity::getTemplateId)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        Map<UUID, String> out = new HashMap<>();
        for (WorkoutTemplateEntity t : templateRepo.findAllById(ids)) {
            out.put(t.getId(), t.getName());
        }
        return out;
    }

    private WeeklyPlanSummaryDto toSummary(WeeklyPlanEntity e) {
        return new WeeklyPlanSummaryDto(
            e.getId(),
            e.getIsoYear(),
            e.getIsoWeek(),
            e.getTitle(),
            computeProgress(e.getDays()),
            e.getUpdatedAt() == null ? Instant.now() : e.getUpdatedAt()
        );
    }

    private WeeklyPlanDto toDetail(WeeklyPlanEntity e, Map<UUID, String> templateNames) {
        List<DailyPlanDto> days = e.getDays().stream()
            .map(d -> toDayDto(d, d.getTemplateId() == null ? null : templateNames.get(d.getTemplateId())))
            .toList();
        List<LocalDate> dates = IsoWeek.datesOf(e.getIsoYear(), e.getIsoWeek());
        LocalDate weekStart = dates.get(0);
        LocalDate weekEnd = dates.get(6);
        return new WeeklyPlanDto(
            e.getId(),
            e.getIsoYear(),
            e.getIsoWeek(),
            weekStart,
            weekEnd,
            e.getTitle(),
            e.getGoalMd(),
            days,
            computeProgress(e.getDays()),
            e.getCreatedAt(),
            e.getUpdatedAt()
        );
    }

    private DailyPlanDto toDayDto(DailyPlanEntity d, String templateName) {
        return new DailyPlanDto(
            d.getId(),
            d.getPlanDate(),
            d.getWeekday(),
            d.getTargetText(),
            d.getTemplateId(),
            d.getTemplateVersion(),
            templateName,
            d.getNotesMd(),
            d.getStatus(),
            d.getActualSessionId(),
            d.getUpdatedAt()
        );
    }

    private WeeklyPlanProgress computeProgress(List<DailyPlanEntity> days) {
        if (days == null || days.isEmpty()) return WeeklyPlanProgress.empty();
        int planned = 0, done = 0, partial = 0, skipped = 0, rescheduled = 0;
        for (DailyPlanEntity d : days) {
            DailyPlanStatus s = d.getStatus() == null ? DailyPlanStatus.PLANNED : d.getStatus();
            switch (s) {
                case PLANNED -> planned++;
                case DONE -> done++;
                case PARTIAL -> partial++;
                case SKIPPED -> skipped++;
                case RESCHEDULED -> rescheduled++;
            }
        }
        return new WeeklyPlanProgress(days.size(), planned, done, partial, skipped, rescheduled);
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
