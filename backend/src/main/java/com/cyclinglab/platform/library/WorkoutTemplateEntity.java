package com.cyclinglab.platform.library;

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
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Workout template, see doc/ARCHITECTURE.md §15.1.2 and §6.3.
 */
@Entity
@Table(name = "workout_template")
@EntityListeners(AuditingEntityListener.class)
@Filter(name = "tenantFilter", condition = "user_id = :userId")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class WorkoutTemplateEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private UserEntity user;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 16)
    private WorkoutCategory category;

    @Column(name = "intensity", length = 32)
    private String intensity;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", nullable = false, columnDefinition = "jsonb")
    private List<String> tags = List.of();

    @Column(name = "description_md", columnDefinition = "text")
    private String descriptionMd;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "structure_json", nullable = false, columnDefinition = "jsonb")
    private String structureJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 16)
    private WorkoutSource source = WorkoutSource.MANUAL;

    @Column(name = "is_archived", nullable = false)
    private Boolean isArchived = false;

    @Column(name = "current_version", nullable = false)
    private Integer currentVersion = 1;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}