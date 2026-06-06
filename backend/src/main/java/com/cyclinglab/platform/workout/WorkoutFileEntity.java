package com.cyclinglab.platform.workout;

import com.cyclinglab.platform.user.UserEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
 * A workout file in a trainer-friendly format (ZWO today, ERG/MRC tomorrow).
 * The {@code xml} field holds the raw, importable payload. Tenant isolation via
 * the {@code tenantFilter} Hibernate filter.
 */
@Entity
@Table(name = "workout_file")
@EntityListeners(AuditingEntityListener.class)
@Filter(name = "tenantFilter", condition = "user_id = :userId")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class WorkoutFileEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private UserEntity user;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "sport_type", nullable = false, length = 16)
    private String sportType = "bike";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", nullable = false, columnDefinition = "jsonb")
    private List<String> tags = List.of();

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "xml", nullable = false, columnDefinition = "text")
    private String xml;

    @Column(name = "source_template_id")
    private UUID sourceTemplateId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "structure_json", nullable = false, columnDefinition = "jsonb")
    private String structureJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false, length = 16)
    private WorkoutFileFormat format = WorkoutFileFormat.ZWO;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}