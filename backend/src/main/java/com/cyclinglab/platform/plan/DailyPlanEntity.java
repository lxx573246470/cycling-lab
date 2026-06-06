package com.cyclinglab.platform.plan;

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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A single day inside a {@link WeeklyPlanEntity}. The template reference is
 * stored as a raw UUID (not a {@code @ManyToOne}) on purpose: the library may
 * archive / replace a template after a day was planned, and we still want to
 * render the original name and version snapshot for the historical record. The
 * service layer resolves the live template via the library repository and
 * falls back to the snapshot if it is gone.
 */
@Entity
@Table(name = "daily_plan")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class DailyPlanEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "weekly_plan_id", nullable = false, updatable = false)
    private WeeklyPlanEntity weeklyPlan;

    @Column(name = "plan_date", nullable = false, updatable = false)
    private LocalDate planDate;

    @Column(name = "weekday", nullable = false, updatable = false)
    private Integer weekday;

    @Column(name = "target_text", columnDefinition = "text")
    private String targetText;

    @Column(name = "template_id")
    private UUID templateId;

    @Column(name = "template_version")
    private Integer templateVersion;

    @Column(name = "notes_md", columnDefinition = "text")
    private String notesMd;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private DailyPlanStatus status = DailyPlanStatus.PLANNED;

    /** Reserved for M3 (FIT upload + analysis). Nullable today. */
    @Column(name = "actual_session_id")
    private UUID actualSessionId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}