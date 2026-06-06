package com.cyclinglab.platform.training;

import com.cyclinglab.platform.user.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.Filter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A user-uploaded FIT activity file. The raw blob lives in
 * {@link com.cyclinglab.platform.training.storage.StorageService}; this row
 * only stores metadata. The {@code status} field drives the async parsing
 * lifecycle (PENDING -> PARSING -> READY / FAILED).
 */
@Entity
@Table(name = "training_file")
@EntityListeners(AuditingEntityListener.class)
@Filter(name = "tenantFilter", condition = "user_id = :userId")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class TrainingFileEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private UserEntity user;

    @Column(name = "iso_year", nullable = false)
    private Integer isoYear;

    @Column(name = "iso_week", nullable = false)
    private Integer isoWeek;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "object_key", nullable = false, length = 512)
    private String objectKey;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "sha256", nullable = false, length = 64)
    private String sha256;

    @Column(name = "sport_type", nullable = false, length = 32)
    private String sportType = "cycling";

    @Column(name = "recorded_at")
    private Instant recordedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private TrainingFileStatus status = TrainingFileStatus.PENDING;

    @Column(name = "failure_message", columnDefinition = "text")
    private String failureMessage;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}