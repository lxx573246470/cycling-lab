package com.cyclinglab.platform.workout;

import com.cyclinglab.platform.library.dto.PageResponse;
import com.cyclinglab.platform.workout.dto.WorkoutFileCreateRequest;
import com.cyclinglab.platform.workout.dto.WorkoutFileDto;
import com.cyclinglab.platform.workout.dto.WorkoutFileSummaryDto;
import jakarta.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workout-files")
@RequiredArgsConstructor
public class WorkoutFileController {

    private final WorkoutFileService service;

    @GetMapping
    public PageResponse<WorkoutFileSummaryDto> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return service.list(page, size);
    }

    @GetMapping("/{id}")
    public WorkoutFileDto get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkoutFileDto create(@Valid @RequestBody WorkoutFileCreateRequest req) {
        return service.create(req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Streams the raw .zwo payload as a downloadable file. The
     * {@code Content-Disposition} filename is the entity name with unsafe
     * characters sanitised and a .zwo suffix appended.
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable UUID id) {
        String xml = service.loadXml(id);
        String filename = service.downloadFilename(id);
        byte[] body = xml.getBytes(StandardCharsets.UTF_8);
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.parseMediaType("application/xml; charset=utf-8"));
        h.setContentDisposition(
            ContentDisposition.attachment()
                .filename(URLEncoder.encode(filename, StandardCharsets.UTF_8))
                .build()
        );
        h.setContentLength(body.length);
        return new ResponseEntity<>(body, h, HttpStatus.OK);
    }
}