package com.cyclinglab.platform.plan;

import com.cyclinglab.platform.library.dto.PageResponse;
import com.cyclinglab.platform.plan.dto.DailyPlanDto;
import com.cyclinglab.platform.plan.dto.DailyPlanUpdateRequest;
import com.cyclinglab.platform.plan.dto.WeeklyPlanCreateRequest;
import com.cyclinglab.platform.plan.dto.WeeklyPlanDto;
import com.cyclinglab.platform.plan.dto.WeeklyPlanSummaryDto;
import com.cyclinglab.platform.plan.dto.WeeklyPlanUpdateRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Weekly-plan REST surface. All endpoints are tenant-scoped; the service uses
 * the {@code tenantFilter} Hibernate filter plus owner-scoped repository
 * lookups for defense in depth.
 */
@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
public class WeeklyPlanController {

    private final WeeklyPlanService service;

    @GetMapping("/weeks")
    public PageResponse<WeeklyPlanSummaryDto> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return service.list(page, size);
    }

    @GetMapping("/weeks/{id}")
    public WeeklyPlanDto get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping("/weeks")
    @ResponseStatus(HttpStatus.CREATED)
    public WeeklyPlanDto create(@Valid @RequestBody WeeklyPlanCreateRequest req) {
        return service.create(req);
    }

    @PutMapping("/weeks/{id}")
    public WeeklyPlanDto update(
        @PathVariable UUID id,
        @Valid @RequestBody WeeklyPlanUpdateRequest req
    ) {
        return service.update(id, req);
    }

    @DeleteMapping("/weeks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/weeks/{weekId}/days/{dayId}")
    public DailyPlanDto updateDay(
        @PathVariable UUID weekId,
        @PathVariable UUID dayId,
        @Valid @RequestBody DailyPlanUpdateRequest req
    ) {
        return service.updateDay(weekId, dayId, req);
    }
}