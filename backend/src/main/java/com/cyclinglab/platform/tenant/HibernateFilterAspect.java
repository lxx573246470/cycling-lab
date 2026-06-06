package com.cyclinglab.platform.tenant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.UUID;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

/**
 * Enables the {@code tenantFilter} Hibernate filter on every @Transactional
 * service call so that any query through the JPA repository of a tenant-scoped
 * entity is automatically constrained to {@code user_id = :userId}. This is the
 * "Hibernate filter" mechanism called out in doc/ARCHITECTURE.md §15.1.3.
 *
 * <p>Native queries are not affected; the design flags this risk and M1 avoids
 * native queries for tenant-scoped reads.
 */
@Aspect
@Component
public class HibernateFilterAspect {

    public static final String FILTER_NAME = "tenantFilter";
    public static final String FILTER_PARAM = "userId";

    @PersistenceContext
    private EntityManager em;

    @Before(
        "execution(public * org.springframework.data.repository.Repository+.*(..))"
        + " || (@within(org.springframework.stereotype.Service) "
        + "     && (execution(public * *(..)) "
        + "         || @annotation(org.springframework.transaction.annotation.Transactional)))"
    )
    public void enableTenantFilter() {
        UUID userId = TenantContext.getCurrentUserIdOrNull();
        if (userId == null) {
            return;
        }
        Session session = em.unwrap(Session.class);
        session.enableFilter(FILTER_NAME)
            .setParameter(FILTER_PARAM, userId);
    }
}
