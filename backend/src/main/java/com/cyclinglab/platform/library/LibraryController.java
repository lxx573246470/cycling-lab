package com.cyclinglab.platform.library;

import com.cyclinglab.platform.library.dto.CategoryDto;
import com.cyclinglab.platform.library.dto.DuplicateRequest;
import com.cyclinglab.platform.library.dto.PageResponse;
import com.cyclinglab.platform.library.dto.WorkoutTemplateCreateRequest;
import com.cyclinglab.platform.library.dto.WorkoutTemplateDto;
import com.cyclinglab.platform.library.dto.WorkoutTemplateListItem;
import com.cyclinglab.platform.library.dto.WorkoutTemplatePatchRequest;
import com.cyclinglab.platform.library.dto.WorkoutTemplatePutRequest;
import com.cyclinglab.platform.library.dto.WorkoutTemplateVersionDetail;
import com.cyclinglab.platform.library.dto.WorkoutTemplateVersionSummary;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/library")
@RequiredArgsConstructor
public class LibraryController {

    private final LibraryService libraryService;

    @GetMapping("/templates")
    public PageResponse<WorkoutTemplateListItem> list(
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String tag,
        @RequestParam(defaultValue = "false") boolean archived,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return libraryService.list(category, q, tag, archived, page, size);
    }

    @GetMapping("/templates/{id}")
    public WorkoutTemplateDto get(@PathVariable UUID id) {
        return libraryService.get(id);
    }

    @PostMapping("/templates")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkoutTemplateDto create(@Valid @RequestBody WorkoutTemplateCreateRequest req) {
        return libraryService.create(req);
    }

    @PutMapping("/templates/{id}")
    public WorkoutTemplateDto replace(
        @PathVariable UUID id,
        @Valid @RequestBody WorkoutTemplatePutRequest req
    ) {
        return libraryService.replace(id, req);
    }

    @PatchMapping("/templates/{id}")
    public WorkoutTemplateDto patch(
        @PathVariable UUID id,
        @Valid @RequestBody WorkoutTemplatePatchRequest req
    ) {
        return libraryService.patch(id, req);
    }

    @DeleteMapping("/templates/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> archive(@PathVariable UUID id) {
        libraryService.archive(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/templates/{id}/versions")
    public List<WorkoutTemplateVersionSummary> versions(@PathVariable UUID id) {
        return libraryService.versions(id);
    }

    @GetMapping("/templates/{id}/versions/{version}")
    public WorkoutTemplateVersionDetail version(
        @PathVariable UUID id,
        @PathVariable int version
    ) {
        return libraryService.version(id, version);
    }

    @GetMapping("/categories")
    public List<CategoryDto> categories() {
        return libraryService.categories();
    }

    @PostMapping("/templates/{id}/duplicate")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkoutTemplateDto duplicate(
        @PathVariable UUID id,
        @RequestBody(required = false) DuplicateRequest req
    ) {
        return libraryService.duplicate(id, req);
    }

    @GetMapping("/category-counts")
    public Map<String, Long> categoryCounts() {
        return libraryService.categoryCounts();
    }
}
