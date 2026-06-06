package com.cyclinglab.platform.library;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
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
 * Immutable snapshot of a template's {@code structure_json} at a given
 * version. Version 1 is written when the template is first created; later
 * versions are written when the structure is replaced via PUT.
 */
@Entity
@Table(name = "workout_template_version")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class WorkoutTemplateVersionEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "template_id", nullable = false, updatable = false)
    private UUID templateId;

    @Column(name = "version", nullable = false, updatable = false)
    private Integer version;

    @JdbcTypeCode(SqlTypes.JSON)
    @Lob
    @Column(name = "structure_json", nullable = false, columnDefinition = "jsonb")
    private String structureJson;

    @Column(name = "change_note", length = 255)
    private String changeNote;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
