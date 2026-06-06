package com.cyclinglab.platform.importer;

import com.cyclinglab.platform.library.LibraryService;
import com.cyclinglab.platform.library.WorkoutCategory;
import com.cyclinglab.platform.library.WorkoutSource;
import com.cyclinglab.platform.library.dto.WorkoutTemplateCreateRequest;
import com.cyclinglab.platform.library.exception.TemplateNameConflictException;
import com.cyclinglab.platform.profile.dto.RiderProfileUpsertRequest;
import com.cyclinglab.platform.profile.ProfileService;
import com.cyclinglab.platform.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

/**
 * M1 importer: scans the legacy {@code profile/} and {@code plans/library/}
 * trees (passed as paths) and creates {@code rider_profile} /
 * {@code workout_template} records for the current user.
 *
 * <p>Strategy is "best-effort":
 * <ul>
 *   <li>Profile: read every key in the frontmatter section and map the few
 *       we know about; unknowns go to a warning.</li>
 *   <li>Templates: the {@code 训练结构} table is parsed to extract a
 *       warmup/steady/cooldown structure. If the file only has a free-text
 *       structure (e.g. "Z2"), we emit a single placeholder block and flag
 *       it as {@code 待补全} via a warning.</li>
 * </ul>
 *
 * <p>All actions are tenant-scoped to {@link TenantContext#getCurrentUserId()}.
 */
@Service
@RequiredArgsConstructor
public class MarkdownImporter {

    private final ProfileService profileService;
    private final LibraryService libraryService;
    private final ObjectMapper objectMapper;

    /**
     * Imports the legacy profile + library trees. Files are mapped to the
     * current user; the existing database state is left as-is (md is read as
     * "seed data"). The M1 design (§15.1.5) calls this out and recommends
     * "mark imported md as archived" out-of-band.
     */
    // NOT @Transactional: each profile/template create is its own
    // transaction (in LibraryService / ProfileService). A failure on one
    // import row must not poison the others.
    public ImportReport importFromPaths(Path profileMd, Path libraryDir) throws IOException {
        ImportReport.RiderProfileSummary profileSummary = Optional.ofNullable(profileMd)
            .filter(Files::exists)
            .map(this::importProfile)
            .orElseGet(() -> new ImportReport.RiderProfileSummary("SKIPPED", List.of("profile path not provided or not found")));

        List<ImportReport.TemplateSummary> templateSummaries = new ArrayList<>();
        if (libraryDir != null && Files.isDirectory(libraryDir)) {
            try (var stream = Files.walk(libraryDir)) {
                List<Path> files = stream.filter(p -> p.toString().endsWith(".md")).toList();
                for (Path file : files) {
                    templateSummaries.add(importTemplate(file, libraryDir));
                }
            }
        }

        return new ImportReport(profileSummary, templateSummaries);
    }

    private ImportReport.RiderProfileSummary importProfile(Path file) {
        try {
            Parsed parsed = parseMd(file);
            Map<String, Object> fm = parsed.frontmatter;
            List<String> warnings = new ArrayList<>();

            String displayName = stringOr(fm, "title", "Rider");
            Short heightCm = shortOr(fm, "height_cm", null, warnings, "height_cm");
            BigDecimal weightKg = decimalOr(fm, "weight_kg", null, warnings, "weight_kg");
            Short maxHr = shortOr(fm, "max_hr", null, warnings, "max_hr");
            Short ftp = shortOr(fm, "ftp", null, warnings, "ftp");
            Short cadenceLow = shortOr(fm, "cadence_low", (short) 80, warnings, "cadence_low");
            Short cadenceHigh = shortOr(fm, "cadence_high", (short) 90, warnings, "cadence_high");
            Short restingHr = shortOr(fm, "resting_hr", null, warnings, "resting_hr");
            Short thresholdHr = shortOr(fm, "threshold_hr", null, warnings, "threshold_hr");

            if (heightCm == null || weightKg == null || maxHr == null || ftp == null) {
                warnings.add("profile: missing one of height_cm / weight_kg / max_hr / ftp; skipping");
                return new ImportReport.RiderProfileSummary("SKIPPED", warnings);
            }

            RiderProfileUpsertRequest req = new RiderProfileUpsertRequest(
                displayName, heightCm, weightKg, maxHr, restingHr, thresholdHr, ftp,
                cadenceLow, cadenceHigh, List.of(),
                null, null, null,
                Map.of(), Map.of(), false
            );
            profileService.upsert(req);
            return new ImportReport.RiderProfileSummary("IMPORTED", warnings);
        } catch (Exception e) {
            return new ImportReport.RiderProfileSummary("FAILED",
                List.of("profile import failed: " + e.getMessage()));
        }
    }

    private ImportReport.TemplateSummary importTemplate(Path file, Path libraryRoot) {
        try {
            Parsed parsed = parseMd(file);
            Map<String, Object> fm = parsed.frontmatter;
            String rawCategory = stringOr(fm, "category", null);
            String name = stringOr(fm, "title", file.getFileName().toString().replace(".md", ""));
            String intensity = stringOr(fm, "intensity", null);
            @SuppressWarnings("unchecked")
            List<String> tags = (List<String>) fm.getOrDefault("tags", List.of());
            List<String> normalizedTags = tags == null ? List.of() : tags.stream()
                .filter(t -> t != null && !t.isBlank())
                .map(t -> t.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .toList();

            WorkoutCategory category;
            List<String> warnings = new ArrayList<>();
            try {
                category = rawCategory == null ? WorkoutCategory.UNCATEGORIZED : WorkoutCategory.fromCode(rawCategory);
                if (category == WorkoutCategory.UNCATEGORIZED && rawCategory != null) {
                    warnings.add("template: unknown category '" + rawCategory + "', bucketed as 'uncategorized'");
                }
            } catch (IllegalArgumentException ex) {
                category = WorkoutCategory.UNCATEGORIZED;
                warnings.add("template: unknown category '" + rawCategory + "', bucketed as 'uncategorized'");
            }
            if (rawCategory == null) {
                warnings.add("template: no 'category' in frontmatter, bucketed as 'uncategorized'");
            }

            StructureInference inferred = inferStructure(parsed.body, warnings);

            WorkoutTemplateCreateRequest req = new WorkoutTemplateCreateRequest(
                name,
                category.code(),
                intensity,
                parsed.body,
                inferred.json,
                normalizedTags
            );

            try {
                libraryService.create(req);
            } catch (TemplateNameConflictException ex) {
                warnings.add("template: name already exists, skipped");
                return new ImportReport.TemplateSummary(name, category.code(), "SKIPPED", warnings);
            } catch (Exception ex) {
                warnings.add("template: create failed: " + ex.getMessage());
                return new ImportReport.TemplateSummary(name, category.code(), "FAILED", warnings);
            }
            return new ImportReport.TemplateSummary(name, category.code(), "IMPORTED", warnings);
        } catch (Exception e) {
            return new ImportReport.TemplateSummary(
                file.getFileName().toString(), "uncategorized", "FAILED",
                List.of("template import failed: " + e.getMessage())
            );
        }
    }

    // -- structure inference --------------------------------------------------

    private record StructureInference(String json, long totalDurationSec) {}

    /** Pulls the first {@code 训练结构} table from the body and turns it into
     *  a structure_json payload. Falls back to a single placeholder block. */
    private StructureInference inferStructure(String body, List<String> warnings) {
        if (body == null) body = "";
        List<List<String>> table = extractFirstTable(body, "(?:训练结构|训练安排|间歇结构|训练内容|结构)");

        if (table.isEmpty()) {
            warnings.add("structure: no structured table found; emitted a placeholder block to be filled in by the user");
            String placeholder = "{\"blocks\":[{\"type\":\"steady\",\"durationSec\":1800,\"power\":0.65}]}";
            return new StructureInference(placeholder, 1800);
        }

        // Header row -> [阶段, 时间, 强度, 说明]
        List<String> header = table.get(0);
        int colTime = indexOfAny(header, "时间", "时长");
        int colIntensity = indexOfAny(header, "强度", "功率", "区间", "目标");
        int colStage = indexOfAny(header, "阶段", "区块");

        List<Map<String, Object>> blocks = new ArrayList<>();
        long total = 0;
        for (int i = 1; i < table.size(); i++) {
            List<String> row = table.get(i);
            String stage = colStage >= 0 ? safe(row, colStage) : "";
            String time = colTime >= 0 ? safe(row, colTime) : "";
            String intensity = colIntensity >= 0 ? safe(row, colIntensity) : "";

            long secs = parseDurationSeconds(time);
            if (secs <= 0) continue;
            total += secs;
            String type = inferType(stage, intensity);
            double power = inferPower(intensity);
            // If the row matches the "4 x 3 分钟" pattern we infer an
            // intervals block with a default 50% off period. Otherwise
            // we emit a single warmup / steady / cooldown / rest block.
            Map<String, Object> block = new LinkedHashMap<>();
            if ("intervals".equals(type) && time != null && time.matches(".*[0-9]+\\s*[xX\u00d7]\\s*[0-9]+.*")) {
                long onSecs = secs;
                long offSecs = Math.max(60, onSecs / 2);
                block.put("type", "intervals");
                block.put("repeats", 1);
                java.util.Map<String, Object> onSeg = new LinkedHashMap<>();
                onSeg.put("durationSec", onSecs);
                onSeg.put("power", Math.min(1.5, power));
                java.util.Map<String, Object> offSeg = new LinkedHashMap<>();
                offSeg.put("durationSec", offSecs);
                offSeg.put("power", 0.45);
                block.put("on", onSeg);
                block.put("off", offSeg);
                total += offSecs;
            } else {
                block.put("type", type);
                block.put("durationSec", secs);
                if ("steady".equals(type) || "rest".equals(type)) {
                    block.put("power", power);
                } else if ("warmup".equals(type)) {
                    block.put("powerLow", Math.max(0.0, power - 0.1));
                    block.put("powerHigh", Math.min(0.9, power + 0.05));
                } else if ("cooldown".equals(type)) {
                    block.put("powerLow", Math.min(0.9, power + 0.05));
                    block.put("powerHigh", Math.max(0.0, power - 0.1));
                }
            }
            blocks.add(block);
        }
        if (blocks.isEmpty()) {
            warnings.add("structure: table found but no parseable rows; emitted a placeholder block");
            String placeholder = "{\"blocks\":[{\"type\":\"steady\",\"durationSec\":1800,\"power\":0.65}]}";
            return new StructureInference(placeholder, 1800);
        }
        try {
            return new StructureInference(objectMapper.writeValueAsString(Map.of("blocks", blocks)), (int) total);
        } catch (Exception e) {
            warnings.add("structure: failed to serialize; emitted placeholder");
            String placeholder = "{\"blocks\":[{\"type\":\"steady\",\"durationSec\":1800,\"power\":0.65}]}";
            return new StructureInference(placeholder, 1800);
        }
    }

    private String inferType(String stage, String intensity) {
        String s = (stage == null ? "" : stage) + " " + (intensity == null ? "" : intensity);
        if (s.contains("热身") || s.contains("warmup")) return "warmup";
        if (s.contains("放松") || s.contains("恢复") || s.contains("cooldown")) return "cooldown";
        if (s.contains("间歇") || s.contains("intervals") || s.contains("组")) return "intervals";
        if (s.contains("休息") || s.contains("rest")) return "rest";
        return "steady";
    }

    private double inferPower(String intensity) {
        if (intensity == null) return 0.65;
        String s = intensity.toUpperCase(Locale.ROOT);
        if (s.contains("Z1")) return 0.55;
        if (s.contains("Z5")) return 1.00;
        if (s.contains("Z4")) return 0.90;
        if (s.contains("Z3")) return 0.80;
        if (s.contains("Z2")) return 0.65;
        if (s.contains("FTP")) {
            Matcher m = Pattern.compile("(\\d{2,3})\\s*%").matcher(s);
            if (m.find()) {
                int pct = Integer.parseInt(m.group(1));
                return Math.min(1.5, Math.max(0.0, pct / 100.0));
            }
            return 0.75;
        }
        return 0.65;
    }

    // -- md / table helpers ---------------------------------------------------

    private static final Pattern FRONTMATTER_RE =
        Pattern.compile("^---\\s*\\n(.*?)\\n---\\s*\\n(.*)$", Pattern.DOTALL);

    private Parsed parseMd(Path file) throws IOException {
        String text = Files.readString(file);
        Matcher m = FRONTMATTER_RE.matcher(text);
        if (!m.matches()) {
            return new Parsed(new LinkedHashMap<>(), text);
        }
        Yaml yaml = new Yaml();
        Object loaded = yaml.load(m.group(1));
        @SuppressWarnings("unchecked")
        Map<String, Object> fm = (loaded instanceof Map) ? (Map<String, Object>) loaded : new LinkedHashMap<>();
        return new Parsed(fm, m.group(2));
    }

    private record Parsed(Map<String, Object> frontmatter, String body) {}

    private List<List<String>> extractFirstTable(String body, String headerPattern) {
        Pattern sectionRe = Pattern.compile("(?m)^#+\\s*[^\\n]*(" + headerPattern + ")[^\\n]*\\n([\\s\\S]*?)(?=^#+\\s|\\Z)");
        Matcher m = sectionRe.matcher(body);
        if (!m.find()) return List.of();
        String section = m.group(2);
        List<List<String>> rows = new ArrayList<>();
        for (String line : section.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("|")) continue;
            String[] cells = Arrays.stream(trimmed.split("\\|"))
                .map(String::trim)
                .filter(s -> !s.isEmpty() && !s.matches("^[-:\\s]+$"))
                .toArray(String[]::new);
            if (cells.length == 0) continue;
            rows.add(Arrays.asList(cells));
        }
        return rows;
    }

    private int indexOfAny(List<String> row, String... needles) {
        for (int i = 0; i < row.size(); i++) {
            for (String n : needles) {
                if (row.get(i).contains(n)) return i;
            }
        }
        return -1;
    }

    private String safe(List<String> row, int idx) {
        return (idx >= 0 && idx < row.size()) ? row.get(idx) : "";
    }

    /** Parses "10-15 分钟" / "3 x 8-12 分钟" / "45 分钟" into a single
     *  seconds estimate (midpoint of any range). */
    long parseDurationSeconds(String s) {
        if (s == null) return 0;
        s = s.replace(" ", "");
        if (s.isEmpty()) return 0;
        // repeat form: 3 x 8 分钟 -> 24 分钟
        Matcher rep = Pattern.compile("(\\d+)\\s*[xX×]\\s*(\\d+)\\s*(分钟|min)?").matcher(s);
        if (rep.find()) {
            int times = Integer.parseInt(rep.group(1));
            int perMin = Integer.parseInt(rep.group(2));
            return times * perMin * 60L;
        }
        // range form: 10-15 分钟 -> 12 分钟
        Matcher range = Pattern.compile("(\\d+)\\s*[-–~]\\s*(\\d+)\\s*(分钟|min|分)?").matcher(s);
        if (range.find()) {
            int a = Integer.parseInt(range.group(1));
            int b = Integer.parseInt(range.group(2));
            int mid = (a + b) / 2;
            return mid * 60L;
        }
        // single value: 45 分钟 / 30 min
        Matcher single = Pattern.compile("(\\d+)\\s*(分钟|min|分|h)?").matcher(s);
        if (single.find()) {
            int v = Integer.parseInt(single.group(1));
            String unit = single.group(2);
            if ("h".equalsIgnoreCase(unit)) return v * 3600L;
            return v * 60L;
        }
        return 0;
    }

    private String stringOr(Map<String, Object> fm, String key, String def) {
        Object v = fm.get(key);
        return v == null ? def : v.toString();
    }

    private Short shortOr(Map<String, Object> fm, String key, Short def, List<String> warnings, String field) {
        Object v = fm.get(key);
        if (v == null) {
            if (def == null) warnings.add("profile: " + field + " not set in frontmatter");
            return def;
        }
        try {
            if (v instanceof Number n) return n.shortValue();
            return Short.parseShort(v.toString());
        } catch (NumberFormatException e) {
            warnings.add("profile: " + field + " is not a number (" + v + ")");
            return def;
        }
    }

    private BigDecimal decimalOr(Map<String, Object> fm, String key, BigDecimal def, List<String> warnings, String field) {
        Object v = fm.get(key);
        if (v == null) {
            if (def == null) warnings.add("profile: " + field + " not set in frontmatter");
            return def;
        }
        try {
            if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
            return new BigDecimal(v.toString());
        } catch (NumberFormatException e) {
            warnings.add("profile: " + field + " is not a number (" + v + ")");
            return def;
        }
    }
}