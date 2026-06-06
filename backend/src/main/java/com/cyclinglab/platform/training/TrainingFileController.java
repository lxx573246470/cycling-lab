package com.cyclinglab.platform.training;

import com.cyclinglab.platform.training.dto.TrainingFileDetailDto;
import com.cyclinglab.platform.training.dto.TrainingFileSummaryDto;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/trainings/files")
@RequiredArgsConstructor
public class TrainingFileController {

    private final TrainingService service;

    @GetMapping
    public TrainingService.PageResponse<TrainingFileSummaryDto> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return service.list(page, size);
    }

    @GetMapping("/{id}")
    public TrainingFileDetailDto get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TrainingFileDetailDto upload(
        @RequestParam("file") MultipartFile file
    ) {
        return service.upload(file);
    }

    @GetMapping("/{id}/samples")
    public TrainingService.SamplePage samples(
        @PathVariable UUID id,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "200") int size
    ) {
        return service.samples(id, page, size);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
