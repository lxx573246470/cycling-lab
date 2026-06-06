package com.cyclinglab.platform.training;

import com.cyclinglab.platform.training.analyzer.AnalysisResult;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Parsed session summary for a {@link TrainingFileEntity}. One session per
 * file. The per-second record stream lives in
 * {@code TrainingRecordSampleEntity}, not here.
 */
@Entity
@Table(name = "training_session")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class TrainingSessionEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_id", nullable = false, updatable = false)
    private TrainingFileEntity file;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private com.cyclinglab.platform.user.UserEntity user;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "duration_sec")
    private Integer durationSec;

    @Column(name = "distance_m")
    private Double distanceM;

    @Column(name = "energy_kj")
    private Double energyKj;

    @Column(name = "avg_hr")
    private Short avgHr;

    @Column(name = "max_hr")
    private Short maxHr;

    @Column(name = "avg_power")
    private Short avgPower;

    @Column(name = "max_power")
    private Short maxPower;

    @Column(name = "normalized_power")
    private Short normalizedPower;

    @Column(name = "intensity_factor")
    private Double intensityFactor;

    @Column(name = "training_stress_score")
    private Double trainingStressScore;

    @Column(name = "avg_cadence")
    private Short avgCadence;

    @Column(name = "max_cadence")
    private Short maxCadence;

    @Column(name = "hr_drift_pct")
    private Double hrDriftPct;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "hr_zone_distribution", columnDefinition = "jsonb")
    private String hrZoneDistribution;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "power_zone_distribution", columnDefinition = "jsonb")
    private String powerZoneDistribution;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cadence_zone_distribution", columnDefinition = "jsonb")
    private String cadenceZoneDistribution;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ten_min_segments", columnDefinition = "jsonb")
    private String tenMinSegments;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "best_rolling", columnDefinition = "jsonb")
    private String bestRolling;

    @Column(name = "notes_md", columnDefinition = "text")
    private String notesMd;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static TrainingSessionEntity from(UUID id, TrainingFileEntity file, com.cyclinglab.platform.user.UserEntity user, AnalysisResult r) {
        TrainingSessionEntity e = new TrainingSessionEntity();
        e.setId(id);
        e.setFile(file);
        e.setUser(user);
        e.setStartedAt(r.startedAt());
        e.setDurationSec(r.durationSec());
        e.setDistanceM(r.distanceM());
        e.setEnergyKj(r.energyKj());
        e.setAvgHr((short) r.avgHr());
        e.setMaxHr((short) r.maxHr());
        e.setAvgPower((short) r.avgPower());
        e.setMaxPower((short) r.maxPower());
        e.setNormalizedPower((short) r.normalizedPower());
        e.setIntensityFactor(r.intensityFactor());
        e.setTrainingStressScore(r.trainingStressScore());
        e.setAvgCadence((short) r.avgCadence());
        e.setMaxCadence((short) r.maxCadence());
        e.setHrDriftPct(r.hrDriftPct());
        e.setHrZoneDistribution(r.hrZoneDistributionJson());
        e.setPowerZoneDistribution(r.powerZoneDistributionJson());
        e.setCadenceZoneDistribution(r.cadenceZoneDistributionJson());
        e.setTenMinSegments(r.tenMinSegmentsJson());
        e.setBestRolling(r.bestRollingJson());
        return e;
    }
}