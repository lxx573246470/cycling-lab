package com.cyclinglab.platform.review;

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
import java.time.LocalDate;
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
 * A retrospective for either a single ISO week ({@code scope = WEEK}) or a
 * longer training phase ({@code scope = PHASE}). The {@code metrics} JSONB
 * column holds derived numbers (weekly TSS, ride counts, distance, average
 * power, etc.) so the page can show a summary block without re-deriving
 * anything client-side.
 */
@Entity
@Table(name = "review")
@EntityListeners(AuditingEntityListener.class)
@Filter(name = "tenantFilter", condition = "user_id = :userId")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class ReviewEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 16)
    private ReviewScope scope;

    @Column(name = "scope_id")
    private UUID scopeId;

    @Column(name = "iso_year")
    private Short isoYear;

    @Column(name = "iso_week")
    private Short isoWeek;

    @Column(name = "period_start")
    private LocalDate periodStart;

    @Column(name = "period_end")
    private LocalDate periodEnd;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content_md", nullable = false, columnDefinition = "text")
    private String contentMd = "";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metrics", columnDefinition = "jsonb")
    private String metrics;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
