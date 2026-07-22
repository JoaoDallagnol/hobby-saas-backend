ALTER TABLE sessions
    ADD COLUMN location_label VARCHAR(150);

UPDATE sessions
SET location_label = 'Local registrado'
WHERE place_id IS NOT NULL;

ALTER TABLE places
    ADD COLUMN validated_at TIMESTAMPTZ NOT NULL DEFAULT TIMESTAMPTZ '1970-01-01 00:00:00+00';

ALTER TABLE places
    DROP COLUMN name,
    DROP COLUMN lat,
    DROP COLUMN lng;

ALTER TABLE sessions
    ADD CONSTRAINT ck_sessions_location_consistency CHECK (
        (place_id IS NULL AND location_label IS NULL)
        OR (place_id IS NOT NULL AND location_label IS NOT NULL AND btrim(location_label) <> '')
    );
