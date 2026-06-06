package com.cyclinglab.platform.profile;

import com.cyclinglab.platform.profile.RiderProfileEntity.Bike;
import com.cyclinglab.platform.profile.dto.DerivedZonesResponse;
import com.cyclinglab.platform.profile.dto.DerivedZonesResponse.CadenceRange;
import com.cyclinglab.platform.profile.dto.DerivedZonesResponse.HrZone;
import com.cyclinglab.platform.profile.dto.DerivedZonesResponse.PowerZone;
import com.cyclinglab.platform.profile.dto.RiderProfileDto;
import com.cyclinglab.platform.profile.dto.RiderProfilePatchRequest;
import com.cyclinglab.platform.profile.dto.RiderProfileUpsertRequest;
import com.cyclinglab.platform.tenant.TenantContext;
import com.cyclinglab.platform.user.UserEntity;
import com.cyclinglab.platform.user.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Profile service. Multi-tenant by way of the {@code tenantFilter} Hibernate
 * filter (enabled transparently by {@code HibernateFilterAspect}); therefore
 * every read can simply say {@code findById(currentUserId)}.
 */
@Service
@RequiredArgsConstructor
public class ProfileService {

    private static final double[] HR_LOW = {0.50, 0.60, 0.70, 0.80, 0.90, 1.00, 1.20};
    private static final double[] HR_HIGH = {0.60, 0.70, 0.80, 0.90, 1.00, 1.20, 1.50};
    private static final String[] ZONE_NAMES = {
        "Active Recovery", "Endurance", "Tempo", "Threshold", "VO2", "Anaerobic", "Neuromuscular"
    };

    private static final double[] POWER_LOW = {0.00, 0.56, 0.76, 0.91, 1.06, 1.21, 1.50};
    private static final double[] POWER_HIGH = {0.55, 0.75, 0.90, 1.05, 1.20, 1.49, 3.00};

    private final RiderProfileRepository repo;
    private final UserRepository userRepo;
    private final ObjectMapper objectMapper;

    /**
     * Returns the current user's profile, or {@code null} when nothing has
     * been saved yet. Per the design, the controller maps "not found" to
     * 200 + empty object so the frontend can show a "start filling in" CTA.
     */
    @Transactional(readOnly = true)
    public RiderProfileDto getCurrent() {
        UUID userId = TenantContext.getCurrentUserId();
        return repo.findById(userId).map(this::toDto).orElse(null);
    }

    @Transactional
    public RiderProfileDto upsert(RiderProfileUpsertRequest req) {
        UUID userId = TenantContext.getCurrentUserId();
        UserEntity user = userRepo.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Current user not found"));
        RiderProfileEntity entity = repo.findById(userId).orElseGet(() -> {
            RiderProfileEntity fresh = new RiderProfileEntity();
            fresh.setUser(user);
            return fresh;
        });
        applyUpsert(entity, req);
        RiderProfileEntity saved = repo.save(entity);
        return toDto(saved);
    }

    @Transactional
    public RiderProfileDto patch(RiderProfilePatchRequest req) {
        UUID userId = TenantContext.getCurrentUserId();
        RiderProfileEntity entity = repo.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Profile not yet created; use PUT"));
        applyPatch(entity, req);
        return toDto(repo.save(entity));
    }

    @Transactional(readOnly = true)
    public DerivedZonesResponse derivedZones() {
        UUID userId = TenantContext.getCurrentUserId();
        RiderProfileEntity p = repo.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Profile not yet created; cannot compute zones"));

        int maxHr = p.getMaxHr();
        int thresholdHr = Optional.ofNullable(p.getThresholdHr())
            .map(Short::intValue)
            .orElse((int) Math.round(maxHr * 0.9));

        List<HrZone> hr = new java.util.ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            hr.add(new HrZone(
                i + 1,
                ZONE_NAMES[i],
                HR_LOW[i],
                HR_HIGH[i],
                (int) Math.round(maxHr * HR_LOW[i]),
                (int) Math.round(maxHr * HR_HIGH[i])
            ));
        }

        int ftp = p.getFtp();
        List<PowerZone> power = new java.util.ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            power.add(new PowerZone(
                i + 1,
                ZONE_NAMES[i],
                POWER_LOW[i],
                POWER_HIGH[i],
                (int) Math.floor(ftp * POWER_LOW[i]),
                (int) Math.floor(ftp * POWER_HIGH[i])
            ));
        }

        return new DerivedZonesResponse(
            hr,
            power,
            new CadenceRange(p.getCadenceLow(), p.getCadenceHigh()),
            ftp,
            maxHr,
            thresholdHr,
            Instant.now().toString()
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> exportDump() {
        RiderProfileDto profile = getCurrent();
        Map<String, Object> out = new HashMap<>();
        out.put("profile", profile);
        if (profile != null) {
            out.put("derivedZones", derivedZones());
        } else {
            out.put("derivedZones", null);
        }
        out.put("exportedAt", Instant.now().toString());
        return out;
    }

    private void applyUpsert(RiderProfileEntity e, RiderProfileUpsertRequest r) {
        e.setDisplayName(r.displayName().trim());
        e.setHeightCm(r.heightCm());
        e.setWeightKg(r.weightKg());
        e.setMaxHr(r.maxHr());
        e.setRestingHr(r.restingHr());
        e.setThresholdHr(r.thresholdHr());
        e.setFtp(r.ftp());
        e.setCadenceLow(r.cadenceLow());
        e.setCadenceHigh(r.cadenceHigh());
        e.setBikes(Optional.ofNullable(r.bikes()).orElseGet(List::of));
        e.setPowerMeter(emptyToNull(r.powerMeter()));
        e.setHrStrap(emptyToNull(r.hrStrap()));
        e.setHeadUnit(emptyToNull(r.headUnit()));
        e.setGoals(Optional.ofNullable(r.goals()).orElseGet(Map::of));
        e.setPreferences(Optional.ofNullable(r.preferences()).orElseGet(Map::of));
        e.setIsPublic(Boolean.TRUE.equals(r.isPublic()));
    }

    private void applyPatch(RiderProfileEntity e, RiderProfilePatchRequest r) {
        if (r.displayName() != null) e.setDisplayName(r.displayName().trim());
        if (r.heightCm() != null) e.setHeightCm(r.heightCm());
        if (r.weightKg() != null) e.setWeightKg(r.weightKg());
        if (r.maxHr() != null) e.setMaxHr(r.maxHr());
        if (r.restingHr() != null) e.setRestingHr(r.restingHr());
        if (r.thresholdHr() != null) e.setThresholdHr(r.thresholdHr());
        if (r.ftp() != null) e.setFtp(r.ftp());
        if (r.cadenceLow() != null) e.setCadenceLow(r.cadenceLow());
        if (r.cadenceHigh() != null) e.setCadenceHigh(r.cadenceHigh());
        if (r.bikes() != null) e.setBikes(r.bikes());
        if (r.powerMeter() != null) e.setPowerMeter(emptyToNull(r.powerMeter()));
        if (r.hrStrap() != null) e.setHrStrap(emptyToNull(r.hrStrap()));
        if (r.headUnit() != null) e.setHeadUnit(emptyToNull(r.headUnit()));
        if (r.goals() != null) e.setGoals(r.goals());
        if (r.preferences() != null) e.setPreferences(r.preferences());
        if (r.isPublic() != null) e.setIsPublic(r.isPublic());
    }

    private RiderProfileDto toDto(RiderProfileEntity e) {
        BigDecimal weight = e.getWeightKg() == null
            ? null
            : e.getWeightKg().setScale(1, RoundingMode.HALF_UP);
        return new RiderProfileDto(
            e.getUserId(),
            e.getDisplayName(),
            e.getHeightCm(),
            weight,
            e.getMaxHr(),
            e.getRestingHr(),
            e.getThresholdHr(),
            e.getFtp(),
            e.getCadenceLow(),
            e.getCadenceHigh(),
            e.getBikes() == null ? List.<Bike>of() : e.getBikes(),
            e.getPowerMeter(),
            e.getHrStrap(),
            e.getHeadUnit(),
            e.getGoals() == null ? Map.of() : e.getGoals(),
            e.getPreferences() == null ? Map.of() : e.getPreferences(),
            Boolean.TRUE.equals(e.getIsPublic()),
            e.getCreatedAt(),
            e.getUpdatedAt()
        );
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    // keep an instance reference so the objectMapper is not "unused" if a
    // future feature wants to serialize the export shape
    @SuppressWarnings("unused")
    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize", e);
        }
    }
}
