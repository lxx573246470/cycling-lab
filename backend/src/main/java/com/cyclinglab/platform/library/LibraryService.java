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
import com.cyclinglab.platform.library.exception.TemplateNameConflictException;
import com.cyclinglab.platform.library.exception.TemplateNotFoundException;
import com.cyclinglab.platform.library.structure.StructureValidator;
import com.cyclinglab.platform.library.structure.WorkoutStructure;
import com.cyclinglab.platform.tenant.TenantContext;
import com.cyclinglab.platform.user.UserEntity;
import com.cyclinglab.platform.user.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityNotFoundException;
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
 * Library / workout-template service. All endpoints are tenant-scoped via the
 * {@code tenantFilter} Hibernate filter and the current user id from
 * {@link TenantContext}.
 */
@Service
@RequiredArgsConstructor
public class LibraryService {

    private final WorkoutTemplateRepository templateRepo;
    private final WorkoutTemplateVersionRepository versionRepo;
    private final UserRepository userRepo;
    private final ObjectMapper objectMapper;
    private final StructureValidator structureValidator;

    public PageResponse<WorkoutTemplateListItem> list(
        String category, String q, String tag, boolean archived,
        int page, int size
    ) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "updatedAt"));

        Page<WorkoutTemplateEntity> result;
        if (category != null && !category.isBlank()) {
            WorkoutCategory cat = WorkoutCategory.fromCode(category);
            result = templateRepo.findAllByCategoryAndIsArchivedFalse(cat, pageable);
        } else {
            result = templateRepo.findAllByIsArchived(archived, pageable);
        }

        List<WorkoutTemplateListItem> items = result.getContent().stream()
            .map(this::toListItem)
            .filter(item -> matchesText(item, q))
            .filter(item -> matchesTag(item, tag))
            .toList();

        return new PageResponse<>(
            items, safePage, safeSize, result.getTotalElements(), result.getTotalPages()
        );
    }

    public WorkoutTemplateDto get(UUID id) {
        UUID userId = TenantContext.getCurrentUserId();
        WorkoutTemplateEntity entity = templateRepo.findByIdAndUser_IdAndIsArchivedFalse(id, userId)
            .or(() -> templateRepo.findByIdAndUser_Id(id, userId))
            .orElseThrow(() -> new TemplateNotFoundException(id));
        return toDto(entity);
    }

    @Transactional
    public WorkoutTemplateDto create(WorkoutTemplateCreateRequest req) {
        UUID userId = TenantContext.getCurrentUserId();
        UserEntity user = userRepo.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Current user not found"));
        ensureNameAvailable(userId, req.name().trim(), null);

        WorkoutStructure.Document structure = structureValidator.parse(req.structureJson());
        String canonical = canonicalize(req.structureJson());

        WorkoutTemplateEntity e = new WorkoutTemplateEntity();
        e.setId(UUID.randomUUID());
        e.setUser(user);
        e.setName(req.name().trim());
        e.setCategory(WorkoutCategory.fromCode(req.category()));
        e.setIntensity(normalizeIntensity(req.intensity()));
        e.setTags(normalizeTags(req.tags()));
        e.setDescriptionMd(req.descriptionMd());
        e.setStructureJson(canonical);
        e.setSource(WorkoutSource.MANUAL);
        e.setIsArchived(false);
        e.setCurrentVersion(1);
        templateRepo.save(e);

        writeVersion(e, canonical, null);
        return toDto(e);
    }

    @Transactional
    public WorkoutTemplateDto replace(UUID id, WorkoutTemplatePutRequest req) {
        UUID userId = TenantContext.getCurrentUserId();
        WorkoutTemplateEntity e = templateRepo.findByIdAndUser_Id(id, userId)
            .orElseThrow(() -> new TemplateNotFoundException(id));
        ensureNameAvailable(userId, req.name().trim(), e.getId());

        WorkoutStructure.Document structure = structureValidator.parse(req.structureJson());
        String canonical = canonicalize(req.structureJson());

        e.setName(req.name().trim());
        e.setCategory(WorkoutCategory.fromCode(req.category()));
        e.setIntensity(normalizeIntensity(req.intensity()));
        e.setTags(normalizeTags(req.tags()));
        e.setDescriptionMd(req.descriptionMd());
        e.setStructureJson(canonical);
        e.setCurrentVersion(e.getCurrentVersion() + 1);
        templateRepo.save(e);

        writeVersion(e, canonical, req.changeNote());
        return toDto(e);
    }

    @Transactional
    public WorkoutTemplateDto patch(UUID id, WorkoutTemplatePatchRequest req) {
        UUID userId = TenantContext.getCurrentUserId();
        WorkoutTemplateEntity e = templateRepo.findByIdAndUser_Id(id, userId)
            .orElseThrow(() -> new TemplateNotFoundException(id));
        if (req.name() != null) {
            ensureNameAvailable(userId, req.name().trim(), e.getId());
            e.setName(req.name().trim());
        }
        if (req.category() != null) e.setCategory(WorkoutCategory.fromCode(req.category()));
        if (req.intensity() != null) e.setIntensity(normalizeIntensity(req.intensity()));
        if (req.tags() != null) e.setTags(normalizeTags(req.tags()));
        if (req.descriptionMd() != null) e.setDescriptionMd(req.descriptionMd());
        if (req.archived() != null) e.setIsArchived(req.archived());
        templateRepo.save(e);
        return toDto(e);
    }

    @Transactional
    public void archive(UUID id) {
        UUID userId = TenantContext.getCurrentUserId();
        WorkoutTemplateEntity e = templateRepo.findByIdAndUser_Id(id, userId)
            .orElseThrow(() -> new TemplateNotFoundException(id));
        if (Boolean.TRUE.equals(e.getIsArchived())) {
            // idempotent: archiving an already-archived template is OK (A10)
            return;
        }
        e.setIsArchived(true);
        templateRepo.save(e);
    }

    public List<WorkoutTemplateVersionSummary> versions(UUID id) {
        UUID userId = TenantContext.getCurrentUserId();
        if (!templateRepo.existsById(id) || templateRepo.findByIdAndUser_Id(id, userId).isEmpty()) {
            throw new TemplateNotFoundException(id);
        }
        return versionRepo.findAllByTemplateIdOrderByVersionDesc(id).stream()
            .map(v -> new WorkoutTemplateVersionSummary(
                v.getVersion(), v.getChangeNote(), v.getCreatedAt()
            ))
            .toList();
    }

    public WorkoutTemplateVersionDetail version(UUID id, int version) {
        UUID userId = TenantContext.getCurrentUserId();
        if (!templateRepo.existsById(id) || templateRepo.findByIdAndUser_Id(id, userId).isEmpty()) {
            throw new TemplateNotFoundException(id);
        }
        return versionRepo.findByTemplateIdAndVersion(id, version)
            .map(v -> new WorkoutTemplateVersionDetail(
                v.getVersion(), v.getStructureJson(), v.getChangeNote(), v.getCreatedAt()
            ))
            .orElseThrow(() -> new TemplateNotFoundException(id));
    }

    public List<CategoryDto> categories() {
        return CategoryDto.all();
    }

    @Transactional
    public WorkoutTemplateDto duplicate(UUID id, DuplicateRequest req) {
        UUID userId = TenantContext.getCurrentUserId();
        WorkoutTemplateEntity source = templateRepo.findByIdAndUser_IdAndIsArchivedFalse(id, userId)
            .or(() -> templateRepo.findByIdAndUser_Id(id, userId))
            .orElseThrow(() -> new TemplateNotFoundException(id));
        UserEntity user = userRepo.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Current user not found"));

        String newName = (req == null || req.name() == null || req.name().isBlank())
            ? source.getName() + " (副本)"
            : req.name().trim();
        ensureNameAvailable(userId, newName, null);

        WorkoutTemplateEntity copy = new WorkoutTemplateEntity();
        copy.setId(UUID.randomUUID());
        copy.setUser(user);
        copy.setName(newName);
        copy.setCategory(source.getCategory());
        copy.setIntensity(source.getIntensity());
        copy.setTags(source.getTags() == null ? List.of() : source.getTags());
        copy.setDescriptionMd(source.getDescriptionMd());
        copy.setStructureJson(source.getStructureJson());
        copy.setSource(WorkoutSource.MANUAL);
        copy.setIsArchived(false);
        copy.setCurrentVersion(1);
        templateRepo.save(copy);
        writeVersion(copy, source.getStructureJson(), "Duplicated from " + source.getId());
        return toDto(copy);
    }

    /**
     * Returns the count of templates per category. Used by the dashboard
     * "library overview" widget (§15.1.6).
     */
    public java.util.Map<String, Long> categoryCounts() {
        java.util.Map<String, Long> out = new java.util.LinkedHashMap<>();
        for (WorkoutCategory c : WorkoutCategory.userSelectable()) {
            long count = templateRepo.findAllByCategoryAndIsArchivedFalse(c,
                PageRequest.of(0, 1)).getTotalElements();
            out.put(c.code(), count);
        }
        return out;
    }

    // -- helpers -----------------------------------------------------------------

    private void ensureNameAvailable(UUID userId, String name, UUID excludeId) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        java.util.Optional<WorkoutTemplateEntity> existing = templateRepo
            .findAllByUser_IdAndIsArchivedFalse(userId).stream()
            .filter(t -> t.getName().equalsIgnoreCase(name))
            .filter(t -> excludeId == null || !t.getId().equals(excludeId))
            .findFirst();
        if (existing.isPresent()) {
            throw new TemplateNameConflictException(name, existing.get().getId());
        }
    }

    private void writeVersion(WorkoutTemplateEntity e, String canonicalJson, String changeNote) {
        WorkoutTemplateVersionEntity v = new WorkoutTemplateVersionEntity();
        v.setId(UUID.randomUUID());
        v.setTemplateId(e.getId());
        v.setVersion(e.getCurrentVersion());
        v.setStructureJson(canonicalJson);
        v.setChangeNote(changeNote);
        versionRepo.save(v);
    }

    private String canonicalize(String rawJson) {
        try {
            // round-trip through Jackson so structure_json is always canonical JSON
            return objectMapper.writeValueAsString(objectMapper.readTree(rawJson));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("structure_json is not valid JSON");
        }
    }

    private boolean matchesText(WorkoutTemplateListItem item, String q) {
        if (q == null || q.isBlank()) return true;
        String needle = q.toLowerCase(Locale.ROOT);
        return item.name().toLowerCase(Locale.ROOT).contains(needle);
    }

    private boolean matchesTag(WorkoutTemplateListItem item, String tag) {
        if (tag == null || tag.isBlank()) return true;
        return item.tags() != null && item.tags().stream()
            .anyMatch(t -> t.equalsIgnoreCase(tag));
    }

    private String normalizeIntensity(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
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

    private WorkoutTemplateListItem toListItem(WorkoutTemplateEntity e) {
        WorkoutStructure.Document structure = parseSafely(e.getStructureJson());
        return new WorkoutTemplateListItem(
            e.getId(),
            e.getName(),
            e.getCategory() == null ? null : e.getCategory().code(),
            e.getIntensity(),
            e.getTags() == null ? List.of() : e.getTags(),
            structure.blocks().size(),
            totalDuration(structure),
            Boolean.TRUE.equals(e.getIsArchived()),
            e.getUpdatedAt()
        );
    }

    private WorkoutTemplateDto toDto(WorkoutTemplateEntity e) {
        WorkoutStructure.Document structure = parseSafely(e.getStructureJson());
        return new WorkoutTemplateDto(
            e.getId(),
            e.getName(),
            e.getCategory() == null ? null : e.getCategory().code(),
            e.getIntensity(),
            e.getTags() == null ? List.of() : e.getTags(),
            e.getDescriptionMd(),
            e.getStructureJson(),
            new WorkoutTemplateDto.WorkoutStructureSummaryDto(
                structure.blocks().size(), totalDuration(structure)
            ),
            e.getSource(),
            Boolean.TRUE.equals(e.getIsArchived()),
            e.getCurrentVersion(),
            e.getCreatedAt(),
            e.getUpdatedAt()
        );
    }

    private WorkoutStructure.Document parseSafely(String json) {
        if (json == null || json.isBlank()) {
            return new WorkoutStructure.Document(List.of());
        }
        try {
            ObjectNode root = (ObjectNode) objectMapper.readTree(json);
            var blocksNode = root.get("blocks");
            if (blocksNode == null || !blocksNode.isArray()) {
                return new WorkoutStructure.Document(List.of());
            }
            java.util.List<WorkoutStructure.Block> out = new java.util.ArrayList<>();
            blocksNode.forEach(n -> out.add(StructureSupport.deserializeBlock((ObjectNode) n)));
            return new WorkoutStructure.Document(out);
        } catch (Exception e) {
            return new WorkoutStructure.Document(List.of());
        }
    }

    private long totalDuration(WorkoutStructure.Document d) {
        long total = 0;
        for (WorkoutStructure.Block b : d.blocks()) {
            if (b instanceof WorkoutStructure.WarmupBlock w) total += w.durationSec();
            else if (b instanceof WorkoutStructure.SteadyBlock s) total += s.durationSec();
            else if (b instanceof WorkoutStructure.CooldownBlock c) total += c.durationSec();
            else if (b instanceof WorkoutStructure.RestBlock r) total += r.durationSec();
            else if (b instanceof WorkoutStructure.IntervalsBlock i) {
                total += ((long) i.repeats()) * (i.on().durationSec() + i.off().durationSec());
            }
        }
        return total;
    }
}
