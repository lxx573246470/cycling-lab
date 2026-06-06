-- M2: generated workout files (Zwift .zwo, plus room for future formats)
-- See doc/ARCHITECTURE.md 搂6.6 / 搂7.4.

CREATE TABLE workout_file (
    id                   UUID PRIMARY KEY,
    user_id              UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name                 VARCHAR(128) NOT NULL,
    sport_type           VARCHAR(16)  NOT NULL DEFAULT 'bike',
    tags                 JSONB        NOT NULL DEFAULT '[]'::jsonb,
    description          TEXT,
    xml                  TEXT         NOT NULL,
    -- when the file was generated from a saved template, keep the link so the
    -- UI can re-render if the template is later edited
    source_template_id   UUID REFERENCES workout_template(id) ON DELETE SET NULL,
    -- structure_json kept as a denormalized copy for "rebuild" without
    -- needing the original template
    structure_json       JSONB        NOT NULL,
    format               VARCHAR(16)  NOT NULL DEFAULT 'ZWO',
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT workout_file_format_chk
        CHECK (format IN ('ZWO','ERG','MRC','ZML'))
);

CREATE INDEX idx_workout_file_user_created
    ON workout_file (user_id, created_at DESC);
CREATE INDEX idx_workout_file_source_template
    ON workout_file (source_template_id);