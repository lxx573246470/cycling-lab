package com.cyclinglab.platform.training;

import com.cyclinglab.platform.common.AppProperties;
import com.cyclinglab.platform.plan.IsoWeek;
import com.cyclinglab.platform.tenant.TenantContext;
import com.cyclinglab.platform.training.analyzer.AnalysisResult;
import com.cyclinglab.platform.training.analyzer.FitAnalyzer;
import com.cyclinglab.platform.training.analyzer.Sample;
import com.cyclinglab.platform.training.dto.TrainingFileDetailDto;
import com.cyclinglab.platform.training.dto.TrainingFileSummaryDto;
import com.cyclinglab.platform.training.dto.TrainingSampleDto;
import com.cyclinglab.platform.training.dto.TrainingSessionSummaryDto;
import com.cyclinglab.platform.training.exception.InvalidFitFileException;
import com.cyclinglab.platform.training.exception.TrainingFileNotFoundException;
import com.cyclinglab.platform.training.storage.StorageService;
import com.cyclinglab.platform.user.UserEntity;
import com.cyclinglab.platform.user.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Training file / session service. Coordinates:
 * <ol>
 *   <li>Upload + sha256 + storage write</li>
 *   <li>Synchronous FIT parse via {@link FitAnalyzer}</li>
 *   <li>Persistence of {@link TrainingSessionEntity} and per-second
 *       {@link TrainingRecordSampleEntity} rows</li>
 *   <li>Read paths: list / detail / sample stream</li>
 * </ol>
 *
 * <p>For M3 we parse synchronously inside the upload transaction's outer
 * flow (using a separate transaction for persistence). This keeps tests
 * deterministic; an async executor can be added later.
 */
@Service
@RequiredArgsConstructor
public class TrainingService {

    private static final Logger log = LoggerFactory.getLogger(TrainingService.class);

    private final TrainingFileRepository fileRepo;
    private final TrainingSessionRepository sessionRepo;
    private final TrainingRecordSampleRepository sampleRepo;
    private final UserRepository userRepo;
    private final StorageService storage;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;

    @Transactional
    public TrainingFileDetailDto upload(MultipartFile upload) {
        if (upload == null || upload.isEmpty()) {
            throw new IllegalArgumentException("upload must not be empty");
        }
        String original = upload.getOriginalFilename();
        if (original == null || original.isBlank()) {
            throw new IllegalArgumentException("upload must have a filename");
        }
        String lower = original.toLowerCase();
        if (!lower.endsWith(".fit")) {
            throw new IllegalArgumentException("only .fit files are supported");
        }
        UUID userId = TenantContext.getCurrentUserId();
        UserEntity user = userRepo.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Current user not found"));

        byte[] bytes;
        try {
            bytes = upload.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read upload", e);
        }
        String sha = sha256(bytes);
        if (fileRepo.existsByUser_IdAndSha256(userId, sha)) {
            throw new IllegalArgumentException("file already uploaded (duplicate content)");
        }

        TrainingFileEntity file = new TrainingFileEntity();
        file.setId(UUID.randomUUID());
        file.setUser(user);
        file.setOriginalFilename(original);
        file.setSizeBytes((long) bytes.length);
        file.setSha256(sha);
        file.setSportType("cycling");
        file.setStatus(TrainingFileStatus.PARSING);

        // ISO week defaults to the current date; the parser will overwrite
        // it when it sees a real {@code start_time}.
        int[] yw = IsoWeek.of(java.time.LocalDate.now(java.time.ZoneOffset.UTC));
        file.setIsoYear(yw[0]);
        file.setIsoWeek(yw[1]);
        file.setRecordedAt(Instant.now());

        // Persist metadata first so we have a row to attach the session to.
        // (FK on training_session.file_id requires a known id.)
        String objectKey = userId + "/" + sha + ".fit";
        file.setObjectKey(objectKey);
        try {
            storage.put(objectKey, bytes);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot persist FIT file", e);
        }
        fileRepo.save(file);

        try {
            AnalysisResult r = FitAnalyzer.analyze(storage.resolve(objectKey));
            persistSession(file, user, r);
            file.setStatus(TrainingFileStatus.READY);
            file.setFailureMessage(null);
            if (r.startedAt() != null) {
                file.setRecordedAt(r.startedAt());
                int[] parsed = IsoWeek.of(r.startedAt().atZone(java.time.ZoneOffset.UTC).toLocalDate());
                file.setIsoYear(parsed[0]);
                file.setIsoWeek(parsed[1]);
            }
        } catch (InvalidFitFileException | IOException e) {
            log.warn("FIT parse failed for {}: {}", original, e.getMessage());
            file.setStatus(TrainingFileStatus.FAILED);
            file.setFailureMessage(e.getMessage());
        }
        return toDetail(fileRepo.save(file));
    }

    private void persistSession(TrainingFileEntity file, UserEntity user, AnalysisResult r) {
        TrainingSessionEntity session = TrainingSessionEntity.from(
            UUID.randomUUID(), file, user, r
        );
        sessionRepo.save(session);
        // Persist samples in bulk. A long session can produce > 10000 rows; we
        // chunk to keep individual transactions reasonable.
        List<Sample> samples = r.samples();
        int batch = 500;
        for (int i = 0; i < samples.size(); i += batch) {
            int end = Math.min(samples.size(), i + batch);
            List<TrainingRecordSampleEntity> chunk = new java.util.ArrayList<>(end - i);
            for (int j = i; j < end; j++) {
                chunk.add(TrainingRecordSampleEntity.of(session, samples.get(j)));
            }
            sampleRepo.saveAll(chunk);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<TrainingFileSummaryDto> list(int page, int size) {
        UUID userId = TenantContext.getCurrentUserId();
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(
            safePage, safeSize,
            Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Page<TrainingFileEntity> p = fileRepo.findAllByUser_Id(userId, pageable);
        List<TrainingFileSummaryDto> items = p.getContent().stream()
            .map(this::toSummary)
            .toList();
        return new PageResponse<>(items, safePage, safeSize, p.getTotalElements(), p.getTotalPages());
    }

    @Transactional(readOnly = true)
    public TrainingFileDetailDto get(UUID id) {
        return toDetail(loadOwned(id));
    }

    @Transactional
    public void delete(UUID id) {
        TrainingFileEntity f = loadOwned(id);
        // Delete the session first (cascade clears samples) and the storage
        // object best-effort. The file row itself goes last so we never have
        // a row pointing at a missing object on a failure.
        sessionRepo.findByFile_Id(id).ifPresent(s -> sampleRepo.deleteAllBySession_Id(s.getId()));
        sessionRepo.findByFile_Id(id).ifPresent(sessionRepo::delete);
        String key = f.getObjectKey();
        fileRepo.delete(f);
        try {
            storage.delete(key);
        } catch (IOException e) {
            log.warn("Failed to delete storage object {}: {}", key, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public SamplePage samples(UUID fileId, int page, int size) {
        UUID userId = TenantContext.getCurrentUserId();
        TrainingFileEntity f = fileRepo.findByIdAndUser_Id(fileId, userId)
            .orElseThrow(() -> new TrainingFileNotFoundException(fileId));
        TrainingSessionEntity s = sessionRepo.findByFile_Id(f.getId())
            .orElseThrow(() -> new TrainingFileNotFoundException(fileId));
        int safeSize = Math.min(Math.max(size, 1), 1000);
        int safePage = Math.max(page, 0);
        long total = sampleRepo.countBySession_Id(s.getId());
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<TrainingRecordSampleEntity> p = sampleRepo.findAllBySession_Id(s.getId(), pageable);
        List<TrainingSampleDto> items = p.getContent().stream()
            .map(this::toSample)
            .toList();
        return new SamplePage(s.getId(), items, safePage, safeSize, total);
    }

    // ---- helpers ----------------------------------------------------------

    private TrainingFileEntity loadOwned(UUID id) {
        UUID userId = TenantContext.getCurrentUserId();
        return fileRepo.findByIdAndUser_Id(id, userId)
            .orElseThrow(() -> new TrainingFileNotFoundException(id));
    }

    private TrainingFileSummaryDto toSummary(TrainingFileEntity e) {
        return new TrainingFileSummaryDto(
            e.getId(),
            e.getIsoYear(),
            e.getIsoWeek(),
            e.getOriginalFilename(),
            e.getSportType(),
            e.getSizeBytes(),
            e.getStatus(),
            e.getRecordedAt(),
            e.getCreatedAt()
        );
    }

    private TrainingFileDetailDto toDetail(TrainingFileEntity e) {
        TrainingSessionSummaryDto session = null;
        TrainingSessionEntity s = sessionRepo.findByFile_Id(e.getId()).orElse(null);
        if (s != null) {
            session = toSessionSummary(s);
        }
        return new TrainingFileDetailDto(
            e.getId(),
            e.getIsoYear(),
            e.getIsoWeek(),
            e.getOriginalFilename(),
            e.getSportType(),
            e.getSizeBytes(),
            e.getSha256(),
            e.getStatus(),
            e.getFailureMessage(),
            e.getRecordedAt(),
            e.getCreatedAt(),
            e.getUpdatedAt(),
            session
        );
    }

    private TrainingSessionSummaryDto toSessionSummary(TrainingSessionEntity s) {
        return new TrainingSessionSummaryDto(
            s.getId(),
            s.getStartedAt(),
            s.getDurationSec(),
            s.getDistanceM(),
            s.getEnergyKj(),
            toInt(s.getAvgHr()),
            toInt(s.getMaxHr()),
            toInt(s.getAvgPower()),
            toInt(s.getMaxPower()),
            toInt(s.getNormalizedPower()),
            s.getIntensityFactor(),
            s.getTrainingStressScore(),
            toInt(s.getAvgCadence()),
            toInt(s.getMaxCadence()),
            s.getHrDriftPct(),
            parse(s.getHrZoneDistribution()),
            parse(s.getPowerZoneDistribution()),
            parse(s.getCadenceZoneDistribution()),
            parse(s.getTenMinSegments()),
            parse(s.getBestRolling())
        );
    }

    private TrainingSampleDto toSample(TrainingRecordSampleEntity e) {
        return new TrainingSampleDto(
            e.getTOffsetSec(),
            toInt(e.getHr()),
            toInt(e.getPower()),
            toInt(e.getCadence()),
            e.getSpeedMps(),
            e.getAltitudeM(),
            e.getLat(),
            e.getLon()
        );
    }

    private JsonNode parse(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static Integer toInt(Short s) {
        return s == null ? null : s.intValue();
    }

    private static String sha256(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(bytes);
            return HexFormat.of().formatHex(dig);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}

    public record SamplePage(UUID sessionId, List<TrainingSampleDto> content, int page, int size, long totalElements) {}
}
