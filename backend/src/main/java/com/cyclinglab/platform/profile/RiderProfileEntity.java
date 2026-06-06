package com.cyclinglab.platform.profile;

import com.cyclinglab.platform.user.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.ParamDef;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Rider profile, 1:1 with {@link UserEntity}. See
 * doc/ARCHITECTURE.md §15.1.2 for the field reference.
 */
@Entity
@Table(name = "rider_profile")
@EntityListeners(AuditingEntityListener.class)
@FilterDef(
    name = "tenantFilter",
    parameters = @ParamDef(name = "userId", type = UUID.class)
)
@Filter(name = "tenantFilter", condition = "user_id = :userId")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class RiderProfileEntity {

    @Id
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(name = "display_name", nullable = false, length = 64)
    private String displayName;

    @Column(name = "height_cm", nullable = false)
    private Short heightCm;

    @Column(name = "weight_kg", nullable = false, precision = 5, scale = 1)
    private BigDecimal weightKg;

    @Column(name = "max_hr", nullable = false)
    private Short maxHr;

    @Column(name = "resting_hr")
    private Short restingHr;

    @Column(name = "threshold_hr")
    private Short thresholdHr;

    @Column(name = "ftp", nullable = false)
    private Short ftp;

    @Column(name = "cadence_low", nullable = false)
    private Short cadenceLow;

    @Column(name = "cadence_high", nullable = false)
    private Short cadenceHigh;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "bikes", nullable = false, columnDefinition = "jsonb")
    private List<Bike> bikes = List.of();

    @Column(name = "power_meter", length = 128)
    private String powerMeter;

    @Column(name = "hr_strap", length = 128)
    private String hrStrap;

    @Column(name = "head_unit", length = 128)
    private String headUnit;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "goals", nullable = false, columnDefinition = "jsonb")
    private Map<String, String> goals = Map.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferences", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> preferences = Map.of();

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public record Bike(String name, String type, Double mileageKm, String notes) {
    }
}
