-- M1: rider profile + workout template library
-- See doc/ARCHITECTURE.md §15.1.2 for the full field reference.

CREATE TABLE rider_profile (
    user_id        UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    display_name   VARCHAR(64)  NOT NULL,
    height_cm      SMALLINT     NOT NULL CHECK (height_cm BETWEEN 100 AND 230),
    weight_kg      NUMERIC(5,1) NOT NULL CHECK (weight_kg BETWEEN 30 AND 200),
    max_hr         SMALLINT     NOT NULL CHECK (max_hr BETWEEN 100 AND 230),
    resting_hr     SMALLINT              CHECK (resting_hr IS NULL OR resting_hr BETWEEN 30 AND 120),
    threshold_hr   SMALLINT              CHECK (threshold_hr IS NULL OR threshold_hr < max_hr),
    ftp            SMALLINT     NOT NULL CHECK (ftp BETWEEN 50 AND 600),
    cadence_low    SMALLINT     NOT NULL CHECK (cadence_low BETWEEN 40 AND 130),
    cadence_high   SMALLINT     NOT NULL CHECK (cadence_high BETWEEN 40 AND 130),
    bikes          JSONB        NOT NULL DEFAULT '[]'::jsonb,
    power_meter    VARCHAR(128),
    hr_strap       VARCHAR(128),
    head_unit      VARCHAR(128),
    goals          JSONB        NOT NULL DEFAULT '{}'::jsonb,
    preferences    JSONB        NOT NULL DEFAULT '{}'::jsonb,
    is_public      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT rider_profile_cadence_range_chk CHECK (cadence_low < cadence_high)
);

CREATE TABLE workout_template (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(128) NOT NULL,
    category        VARCHAR(16)  NOT NULL,
    intensity       VARCHAR(32),
    tags            JSONB        NOT NULL DEFAULT '[]'::jsonb,
    description_md  TEXT,
    structure_json  JSONB        NOT NULL,
    source          VARCHAR(16)  NOT NULL DEFAULT 'MANUAL',
    is_archived     BOOLEAN      NOT NULL DEFAULT FALSE,
    current_version INTEGER      NOT NULL DEFAULT 1,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT workout_template_category_chk
        CHECK (category IN ('endurance','recovery','intervals','outdoor','testing','strength','uncategorized')),
    CONSTRAINT workout_template_source_chk
        CHECK (source IN ('MANUAL','IMPORT','AI_SUGGESTED')),
    CONSTRAINT workout_template_unique_name UNIQUE (user_id, name)
);

CREATE INDEX idx_workout_template_user_category
    ON workout_template (user_id, category, is_archived);
CREATE INDEX idx_workout_template_user_updated
    ON workout_template (user_id, updated_at DESC);

CREATE TABLE workout_template_version (
    id             UUID PRIMARY KEY,
    template_id    UUID NOT NULL REFERENCES workout_template(id) ON DELETE CASCADE,
    version        INTEGER NOT NULL,
    structure_json JSONB   NOT NULL,
    change_note    VARCHAR(255),
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT workout_template_version_unique UNIQUE (template_id, version)
);

CREATE INDEX idx_workout_template_version_template
    ON workout_template_version (template_id, version DESC);
