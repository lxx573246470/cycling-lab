-- Align the Flyway schema with the JPA mapping. Hibernate validates Java
-- String(length = 64) as VARCHAR(64), while PostgreSQL reports CHAR(64) as
-- bpchar.
ALTER TABLE training_file
    ALTER COLUMN sha256 TYPE VARCHAR(64);
