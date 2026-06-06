package com.cyclinglab.platform.plan;

import com.cyclinglab.platform.user.UserEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
 * Weekly plan header, see doc/ARCHITECTURE.md 搂6.4 / 搂15.1. Tenant isolation
 * via the {@code tenantFilter} Hibernate filter; daily plans are scoped through
 * their parent (service layer verifies weekly-plan ownership before exposing
 * children).
 */
@Entity
@Table(name = "weekly_plan")
@EntityListeners(AuditingEntityListener.class)
@Filter(name = "tenantFilter", condition = "user_id = :userId")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class WeeklyPlanEntity {

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

    @Column(name = "title", length = 128)
    private String title;

    @Column(name = "goal_md", columnDefinition = "text")
    private String goalMd;

    @OneToMany(
        mappedBy = "weeklyPlan",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @OrderBy("planDate ASC")
    private List<DailyPlanEntity> days = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}