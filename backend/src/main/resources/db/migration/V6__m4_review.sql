-- M4: weekly / phase reviews (per-training-cycle retrospective).
-- See doc/ARCHITECTURE.md section 6.7 (review).

CREATE TABLE review (
    id            UUID PRIMARY KEY,
    user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    scope         VARCHAR(16) NOT NULL,
    scope_id      UUID,
    iso_year      SMALLINT,
    iso_week      SMALLINT,
    period_start  DATE,
    period_end    DATE,
    title         VARCHAR(200) NOT NULL,
    content_md    TEXT NOT NULL DEFAULT '',
    metrics       JSONB,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT review_scope_chk
        CHECK (scope IN ('WEEK', 'PHASE')),
    CONSTRAINT review_iso_chk
        CHECK (iso_year IS NULL OR (iso_year BETWEEN 2000 AND 2100)),
    CONSTRAINT review_week_chk
        CHECK (iso_week IS NULL OR (iso_week BETWEEN 1 AND 53)),
    CONSTRAINT review_period_chk
        CHECK (period_start IS NULL OR period_end IS NULL OR period_end >= period_start)
);

-- A user can have at most one WEEK review per (iso_year, iso_week).
CREATE UNIQUE INDEX review_user_week_unique
    ON review (user_id, iso_year, iso_week)
    WHERE scope = 'WEEK' AND iso_year IS NOT NULL AND iso_week IS NOT NULL;

-- Phase reviews are looked up by their phase id; index for fast lookups.
CREATE INDEX idx_review_user_scope
    ON review (user_id, scope, updated_at DESC);

CREATE INDEX idx_review_user_period
    ON review (user_id, period_start DESC, period_end DESC);