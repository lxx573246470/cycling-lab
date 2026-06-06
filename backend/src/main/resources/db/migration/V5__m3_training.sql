-- M3: training files, parsed sessions, and down-sampled record streams.
-- See doc/ARCHITECTURE.md section 6.5 / 7.3.

CREATE TABLE training_file (
    id                UUID PRIMARY KEY,
    user_id           UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    iso_year          SMALLINT NOT NULL,
    iso_week          SMALLINT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    object_key        VARCHAR(512) NOT NULL,           -- path inside StorageService
    size_bytes        BIGINT NOT NULL,
    sha256            CHAR(64)     NOT NULL,
    sport_type        VARCHAR(32)  NOT NULL DEFAULT 'cycling',
    recorded_at       TIMESTAMP WITH TIME ZONE,
    status            VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    failure_message   TEXT,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT training_file_status_chk
        CHECK (status IN ('PENDING','PARSING','READY','FAILED')),
    CONSTRAINT training_file_sha_unique UNIQUE (user_id, sha256),
    CONSTRAINT training_file_iso_chk
        CHECK (iso_year BETWEEN 2000 AND 2100 AND iso_week BETWEEN 1 AND 53)
);

CREATE INDEX idx_training_file_user_created
    ON training_file (user_id, created_at DESC);
CREATE INDEX idx_training_file_user_status
    ON training_file (user_id, status);

CREATE TABLE training_session (
    id                       UUID PRIMARY KEY,
    file_id                  UUID NOT NULL REFERENCES training_file(id) ON DELETE CASCADE,
    user_id                  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    started_at               TIMESTAMP WITH TIME ZONE,
    duration_sec             INTEGER,
    distance_m               DOUBLE PRECISION,
    energy_kj                DOUBLE PRECISION,
    avg_hr                   SMALLINT,
    max_hr                   SMALLINT,
    avg_power                SMALLINT,
    max_power                SMALLINT,
    normalized_power         SMALLINT,
    intensity_factor         DOUBLE PRECISION,
    training_stress_score    DOUBLE PRECISION,
    avg_cadence              SMALLINT,
    max_cadence              SMALLINT,
    hr_drift_pct             DOUBLE PRECISION,
    hr_zone_distribution     JSONB,
    power_zone_distribution  JSONB,
    cadence_zone_distribution JSONB,
    ten_min_segments         JSONB,
    best_rolling             JSONB,
    notes_md                 TEXT,
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT training_session_file_unique UNIQUE (file_id)
);

CREATE INDEX idx_training_session_user_started
    ON training_session (user_id, started_at DESC);

CREATE TABLE training_record_sample (
    id             BIGSERIAL PRIMARY KEY,
    session_id     UUID NOT NULL REFERENCES training_session(id) ON DELETE CASCADE,
    t_offset_sec   INTEGER NOT NULL,
    hr             SMALLINT,
    power          SMALLINT,
    cadence        SMALLINT,
    speed_mps      DOUBLE PRECISION,
    altitude_m     DOUBLE PRECISION,
    lat            DOUBLE PRECISION,
    lon            DOUBLE PRECISION
);

CREATE INDEX idx_training_sample_session_time
    ON training_record_sample (session_id, t_offset_sec);
