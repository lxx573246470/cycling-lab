package com.cyclinglab.platform.review;

import com.cyclinglab.platform.library.dto.PageResponse;
import com.cyclinglab.platform.plan.IsoWeek;
import com.cyclinglab.platform.review.dto.ReviewCreateRequest;
import com.cyclinglab.platform.review.dto.ReviewDto;
import com.cyclinglab.platform.review.dto.ReviewUpdateRequest;
import com.cyclinglab.platform.review.exception.ReviewConflictException;
import com.cyclinglab.platform.review.exception.ReviewNotFoundException;
import com.cyclinglab.platform.tenant.TenantContext;
import com.cyclinglab.platform.user.UserEntity;
import com.cyclinglab.platform.user.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Review service. Backed by the {@code review} table introduced in M4; one
 * row per user per (scope, iso-year, iso-week) for {@code WEEK} reviews.
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository repo;
    private final UserRepository userRepo;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public PageResponse<ReviewDto> list(int page, int size) {
        UUID userId = TenantContext.getCurrentUserId();
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<ReviewEntity> p = repo.findAllByUser_IdOrderByUpdatedAtDesc(userId, pageable);
        List<ReviewDto> items = p.getContent().stream().map(this::toDto).toList();
        return new PageResponse<>(items, safePage, safeSize, p.getTotalElements(), p.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ReviewDto get(UUID id) {
        return toDto(loadOwned(id));
    }

    @Transactional(readOnly = true)
    public ReviewDto findWeekly(int isoYear, int isoWeek) {
        UUID userId = TenantContext.getCurrentUserId();
        return repo
            .findByUser_IdAndScopeAndIsoYearAndIsoWeek(userId, ReviewScope.WEEK, toShort(isoYear), toShort(isoWeek))
            .map(this::toDto)
            .orElseThrow(() -> new ReviewNotFoundException(
                "No weekly review for " + isoYear + "-W" + isoWeek
            ));
    }

    @Transactional
    public ReviewDto create(ReviewCreateRequest req) {
        UUID userId = TenantContext.getCurrentUserId();
        UserEntity user = userRepo.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Current user not found"));

        if (req.scope() == ReviewScope.WEEK) {
            if (req.isoYear() == null || req.isoWeek() == null) {
                throw new IllegalArgumentException("isoYear and isoWeek are required for WEEK scope");
            }
            // Validate the (year, week) actually exists.
            IsoWeek.datesOf(req.isoYear(), req.isoWeek());
            repo.findByUser_IdAndScopeAndIsoYearAndIsoWeek(
                userId, ReviewScope.WEEK, toShort(req.isoYear()), toShort(req.isoWeek())
            ).ifPresent(existing -> {
                throw new ReviewConflictException(
                    "A WEEK review for " + req.isoYear() + "-W" + req.isoWeek() + " already exists"
                );
            });
        } else {
            if (req.title() == null || req.title().isBlank()) {
                throw new IllegalArgumentException("title is required for PHASE scope");
            }
            if (req.scopeId() == null) {
                throw new IllegalArgumentException("scopeId is required for PHASE scope");
            }
        }

        validatePeriod(req.periodStart(), req.periodEnd());

        ReviewEntity e = new ReviewEntity();
        e.setId(UUID.randomUUID());
        e.setUser(user);
        e.setScope(req.scope());
        e.setScopeId(req.scopeId());
        e.setIsoYear(toNullableShort(req.isoYear()));
        e.setIsoWeek(toNullableShort(req.isoWeek()));
        e.setPeriodStart(req.periodStart());
        e.setPeriodEnd(req.periodEnd());
        e.setTitle(req.title().trim());
        e.setContentMd(req.contentMd() == null ? "" : req.contentMd());
        e.setMetrics(serialize(req.metrics()));
        return toDto(repo.save(e));
    }

    @Transactional
    public ReviewDto update(UUID id, ReviewUpdateRequest req) {
        ReviewEntity e = loadOwned(id);
        if (e.getScope() == ReviewScope.WEEK) {
            if (req.isoYear() == null || req.isoWeek() == null) {
                throw new IllegalArgumentException("isoYear and isoWeek are required for WEEK scope");
            }
            IsoWeek.datesOf(req.isoYear(), req.isoWeek());
        }
        validatePeriod(req.periodStart(), req.periodEnd());
        e.setIsoYear(toNullableShort(req.isoYear()));
        e.setIsoWeek(toNullableShort(req.isoWeek()));
        e.setPeriodStart(req.periodStart());
        e.setPeriodEnd(req.periodEnd());
        e.setTitle(req.title().trim());
        e.setContentMd(req.contentMd() == null ? "" : req.contentMd());
        e.setMetrics(serialize(req.metrics()));
        return toDto(repo.save(e));
    }

    @Transactional
    public void delete(UUID id) {
        ReviewEntity e = loadOwned(id);
        repo.delete(e);
    }

    // ---- helpers ----------------------------------------------------------

    private ReviewEntity loadOwned(UUID id) {
        UUID userId = TenantContext.getCurrentUserId();
        return repo.findByIdAndUser_Id(id, userId)
            .orElseThrow(() -> new ReviewNotFoundException(id));
    }

    private void validatePeriod(LocalDate start, LocalDate end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new IllegalArgumentException("periodEnd must not be before periodStart");
        }
    }

    private String serialize(Object metrics) {
        if (metrics == null) return null;
        try {
            return objectMapper.writeValueAsString(metrics);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("metrics is not serializable");
        }
    }

    private JsonNode parse(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private ReviewDto toDto(ReviewEntity e) {
        return new ReviewDto(
            e.getId(),
            e.getScope(),
            e.getScopeId(),
            toNullableInt(e.getIsoYear()),
            toNullableInt(e.getIsoWeek()),
            e.getPeriodStart(),
            e.getPeriodEnd(),
            e.getTitle(),
            e.getContentMd(),
            parse(e.getMetrics()),
            e.getCreatedAt(),
            e.getUpdatedAt()
        );
    }

    private Short toShort(int value) {
        return (short) value;
    }

    private Short toNullableShort(Integer value) {
        return value == null ? null : value.shortValue();
    }

    private Integer toNullableInt(Short value) {
        return value == null ? null : value.intValue();
    }
}
