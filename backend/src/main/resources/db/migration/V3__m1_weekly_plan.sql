-- M1 phase 2: weekly plan + daily plan (see doc/ARCHITECTURE.md 搂6.4 / 搂15.1)

CREATE TABLE weekly_plan (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    iso_year    SMALLINT NOT NULL CHECK (iso_year BETWEEN 2000 AND 2100),
    iso_week    SMALLINT NOT NULL CHECK (iso_week BETWEEN 1 AND 53),
    title       VARCHAR(128),
    goal_md     TEXT,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT weekly_plan_unique_year_week UNIQUE (user_id, iso_year, iso_week)
);

CREATE INDEX idx_weekly_plan_user_year_week
    ON weekly_plan (user_id, iso_year DESC, iso_week DESC);

CREATE TABLE daily_plan (
    id                UUID PRIMARY KEY,
    weekly_plan_id    UUID NOT NULL REFERENCES weekly_plan(id) ON DELETE CASCADE,
    -- date stored as ISO calendar date (no timezone) representing the planned day
    plan_date         DATE NOT NULL,
    weekday           SMALLINT NOT NULL CHECK (weekday BETWEEN 1 AND 7),
    target_text       TEXT,
    template_id       UUID REFERENCES workout_template(id) ON DELETE SET NULL,
    -- snapshot of template version at the time the day was planned; lets the UI
    -- still resolve the original structure_json even after a template is updated
    template_version  INTEGER,
    notes_md          TEXT,
    status            VARCHAR(16) NOT NULL DEFAULT 'PLANNED',
    -- reserved for M3 (FIT upload + analysis) so the schema is ready; UI may
    -- show the linked session summary once M3 lands
    actual_session_id UUID,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT daily_plan_status_chk
        CHECK (status IN ('PLANNED','DONE','PARTIAL','SKIPPED','RESCHEDULED')),
    CONSTRAINT daily_plan_unique_date_per_week
        UNIQUE (weekly_plan_id, plan_date)
);

CREATE INDEX idx_daily_plan_week_date
    ON daily_plan (weekly_plan_id, plan_date);