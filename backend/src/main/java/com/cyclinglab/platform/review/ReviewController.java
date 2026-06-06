package com.cyclinglab.platform.review;

import com.cyclinglab.platform.library.dto.PageResponse;
import com.cyclinglab.platform.review.dto.ReviewCreateRequest;
import com.cyclinglab.platform.review.dto.ReviewDto;
import com.cyclinglab.platform.review.dto.ReviewUpdateRequest;
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

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService service;

    @GetMapping
    public PageResponse<ReviewDto> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return service.list(page, size);
    }

    @GetMapping("/{id}")
    public ReviewDto get(@PathVariable UUID id) {
        return service.get(id);
    }

    /**
     * Look up the WEEK review for an (isoYear, isoWeek) pair. Returns 404 if
     * the user hasn't written one yet — clients should then show the empty
     * editor instead of an error.
     */
    @GetMapping("/by-week")
    public ReviewDto byWeek(
        @RequestParam int isoYear,
        @RequestParam int isoWeek
    ) {
        return service.findWeekly(isoYear, isoWeek);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewDto create(@Valid @RequestBody ReviewCreateRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public ReviewDto update(
        @PathVariable UUID id,
        @Valid @RequestBody ReviewUpdateRequest req
    ) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}