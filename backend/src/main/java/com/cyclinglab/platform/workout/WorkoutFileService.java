package com.cyclinglab.platform.workout;

import com.cyclinglab.platform.library.WorkoutTemplateEntity;
import com.cyclinglab.platform.library.WorkoutTemplateRepository;
import com.cyclinglab.platform.library.dto.PageResponse;
import com.cyclinglab.platform.library.exception.TemplateNotFoundException;
import com.cyclinglab.platform.library.structure.StructureValidator;
import com.cyclinglab.platform.library.structure.WorkoutStructure;
import com.cyclinglab.platform.tenant.TenantContext;
import com.cyclinglab.platform.user.UserEntity;
import com.cyclinglab.platform.user.UserRepository;
import com.cyclinglab.platform.workout.ZwoGenerator.Header;
import com.cyclinglab.platform.workout.dto.WorkoutFileCreateRequest;
import com.cyclinglab.platform.workout.dto.WorkoutFileDto;
import com.cyclinglab.platform.workout.dto.WorkoutFileSummaryDto;
import com.cyclinglab.platform.workout.exception.WorkoutFileNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Workout-file service. Generates ZWO XML from a structure_json (inline) or
 * from a saved template, persists the result, and exposes both a download
 * stream and a list view.
 */
@Service
@RequiredArgsConstructor
public class WorkoutFileService {

    private final WorkoutFileRepository repo;
    private final WorkoutTemplateRepository templateRepo;
    private final UserRepository userRepo;
    private final StructureValidator structureValidator;
    private final ObjectMapper objectMapper;

    public PageResponse<WorkoutFileSummaryDto> list(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        UUID userId = TenantContext.getCurrentUserId();
        Pageable pageable = PageRequest.of(
            safePage, safeSize,
            Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Page<WorkoutFileEntity> p = repo.findAllByUser_Id(userId, pageable);
        List<WorkoutFileSummaryDto> items = p.getContent().stream()
            .map(this::toSummary)
            .toList();
        return new PageResponse<>(items, safePage, safeSize, p.getTotalElements(), p.getTotalPages());
    }

    public WorkoutFileDto get(UUID id) {
        return toDetail(loadOwned(id));
    }

    @Transactional
    public WorkoutFileDto create(WorkoutFileCreateRequest req) {
        UUID userId = TenantContext.getCurrentUserId();
        UserEntity user = userRepo.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Current user not found"));

        String structureJson;
        UUID sourceTemplateId = req.sourceTemplateId();
        if (sourceTemplateId != null) {
            WorkoutTemplateEntity tpl = templateRepo
                .findByIdAndUser_IdAndIsArchivedFalse(sourceTemplateId, userId)
                .or(() -> templateRepo.findByIdAndUser_Id(sourceTemplateId, userId))
                .orElseThrow(() -> new TemplateNotFoundException(sourceTemplateId));
            structureJson = tpl.getStructureJson();
        } else {
            structureJson = req.structureJson();
        }

        WorkoutStructure.Document doc = structureValidator.parse(structureJson);
        String canonical = canonicalize(structureJson);

        String sport = (req.sportType() == null || req.sportType().isBlank())
            ? "bike"
            : req.sportType().toLowerCase(Locale.ROOT);

        Header header = new Header(
            req.name().trim(),
            "cycling-lab",
            req.description(),
            sport,
            normalizeTags(req.tags())
        );
        String xml = ZwoGenerator.generate(doc, header);

        WorkoutFileEntity e = new WorkoutFileEntity();
        e.setId(UUID.randomUUID());
        e.setUser(user);
        e.setName(header.name());
        e.setSportType(header.sportType());
        e.setTags(header.tags());
        e.setDescription(req.description() == null ? null : req.description().trim());
        e.setXml(xml);
        e.setSourceTemplateId(sourceTemplateId);
        e.setStructureJson(canonical);
        e.setFormat(WorkoutFileFormat.ZWO);

        return toDetail(repo.save(e));
    }

    @Transactional
    public void delete(UUID id) {
        WorkoutFileEntity e = loadOwned(id);
        repo.delete(e);
    }

    /** Returns just the XML body for the download endpoint. */
    public String loadXml(UUID id) {
        return loadOwned(id).getXml();
    }

    /** Returns a filesystem-safe filename for the download endpoint. */
    public String downloadFilename(UUID id) {
        WorkoutFileEntity e = loadOwned(id);
        String safe = e.getName().replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (safe.isEmpty()) safe = "workout";
        return safe + ".zwo";
    }

    // ---- helpers ----------------------------------------------------------

    private WorkoutFileEntity loadOwned(UUID id) {
        UUID userId = TenantContext.getCurrentUserId();
        return repo.findByIdAndUser_Id(id, userId)
            .orElseThrow(() -> new WorkoutFileNotFoundException(id));
    }

    private String canonicalize(String rawJson) {
        try {
            return objectMapper.writeValueAsString(objectMapper.readTree(rawJson));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("structure_json is not valid JSON");
        }
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null) return List.of();
        java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<>();
        for (String raw : tags) {
            if (raw == null) continue;
            String t = raw.trim().toLowerCase(Locale.ROOT);
            if (t.isEmpty()) continue;
            if (t.length() > 32) {
                throw new IllegalArgumentException("tag '" + raw + "' is longer than 32 chars");
            }
            seen.add(t);
        }
        if (seen.size() > 16) {
            throw new IllegalArgumentException("at most 16 tags are allowed");
        }
        return List.copyOf(seen);
    }

    private WorkoutFileSummaryDto toSummary(WorkoutFileEntity e) {
        return new WorkoutFileSummaryDto(
            e.getId(),
            e.getName(),
            e.getSportType(),
            e.getTags() == null ? List.of() : e.getTags(),
            e.getFormat(),
            e.getXml() == null ? 0L : e.getXml().getBytes(StandardCharsets.UTF_8).length,
            e.getSourceTemplateId(),
            e.getCreatedAt()
        );
    }

    private WorkoutFileDto toDetail(WorkoutFileEntity e) {
        return new WorkoutFileDto(
            e.getId(),
            e.getName(),
            e.getSportType(),
            e.getTags() == null ? List.of() : e.getTags(),
            e.getDescription(),
            e.getXml(),
            e.getSourceTemplateId(),
            e.getFormat(),
            e.getXml() == null ? 0L : e.getXml().getBytes(StandardCharsets.UTF_8).length,
            e.getCreatedAt(),
            e.getUpdatedAt()
        );
    }
}