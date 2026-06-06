package com.cyclinglab.platform.training;

import com.cyclinglab.platform.training.analyzer.Sample;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One row per (down-sampled) record of a parsed session. Default resolution
 * matches the source file (typically 1 Hz); long sessions may be further
 * down-sampled by the analyzer before persistence.
 */
@Entity
@Table(name = "training_record_sample")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class TrainingRecordSampleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false, updatable = false)
    private TrainingSessionEntity session;

    @Column(name = "t_offset_sec", nullable = false)
    private Integer tOffsetSec;

    @Column(name = "hr")
    private Short hr;

    @Column(name = "power")
    private Short power;

    @Column(name = "cadence")
    private Short cadence;

    @Column(name = "speed_mps")
    private Double speedMps;

    @Column(name = "altitude_m")
    private Double altitudeM;

    @Column(name = "lat")
    private Double lat;

    @Column(name = "lon")
    private Double lon;

    public static TrainingRecordSampleEntity of(TrainingSessionEntity session, Sample s) {
        TrainingRecordSampleEntity e = new TrainingRecordSampleEntity();
        e.setSession(session);
        e.setTOffsetSec(s.tOffsetSec());
        e.setHr(s.hr() == null ? null : (short) s.hr().intValue());
        e.setPower(s.power() == null ? null : (short) s.power().intValue());
        e.setCadence(s.cadence() == null ? null : (short) s.cadence().intValue());
        e.setSpeedMps(s.speed());
        e.setAltitudeM(s.altitude());
        e.setLat(s.lat());
        e.setLon(s.lon());
        return e;
    }
}