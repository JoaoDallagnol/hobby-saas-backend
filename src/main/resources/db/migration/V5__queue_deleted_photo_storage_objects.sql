ALTER TABLE session_photos
    ADD CONSTRAINT uk_session_photos_storage_key_original UNIQUE (storage_key_original);

CREATE TABLE photo_storage_deletions (
    id UUID PRIMARY KEY,
    storage_key VARCHAR(500) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_error VARCHAR(100)
);

CREATE INDEX idx_photo_storage_deletions_due
    ON photo_storage_deletions (next_attempt_at, created_at);

CREATE FUNCTION queue_deleted_session_photo_objects()
RETURNS trigger AS $$
BEGIN
    INSERT INTO photo_storage_deletions (id, storage_key)
    VALUES (gen_random_uuid(), OLD.storage_key_original)
    ON CONFLICT (storage_key) DO NOTHING;

    IF OLD.storage_key_thumbnail IS NOT NULL THEN
        INSERT INTO photo_storage_deletions (id, storage_key)
        VALUES (gen_random_uuid(), OLD.storage_key_thumbnail)
        ON CONFLICT (storage_key) DO NOTHING;
    END IF;

    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_queue_deleted_session_photo_objects
AFTER DELETE ON session_photos
FOR EACH ROW
EXECUTE FUNCTION queue_deleted_session_photo_objects();
