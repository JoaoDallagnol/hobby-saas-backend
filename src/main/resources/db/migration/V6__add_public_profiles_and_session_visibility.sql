ALTER TABLE users
    ADD COLUMN username VARCHAR(30);

CREATE UNIQUE INDEX uk_users_username_lower
    ON users (lower(username))
    WHERE username IS NOT NULL;

ALTER TABLE sessions
    ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'only_me',
    ADD CONSTRAINT ck_sessions_visibility CHECK (visibility IN ('everyone', 'only_me'));

ALTER TABLE session_photos
    ADD COLUMN storage_scope VARCHAR(20) NOT NULL DEFAULT 'private',
    ADD CONSTRAINT ck_session_photos_storage_scope CHECK (storage_scope IN ('public', 'private'));

CREATE UNIQUE INDEX uk_session_photos_session_id ON session_photos (session_id);

ALTER TABLE photo_storage_deletions
    ADD COLUMN storage_scope VARCHAR(20) NOT NULL DEFAULT 'private';

ALTER TABLE photo_storage_deletions
    DROP CONSTRAINT photo_storage_deletions_storage_key_key;

ALTER TABLE photo_storage_deletions
    ADD CONSTRAINT uk_photo_storage_deletions_scope_key UNIQUE (storage_scope, storage_key);

DROP TRIGGER trg_queue_deleted_session_photo_objects ON session_photos;
DROP FUNCTION queue_deleted_session_photo_objects();

CREATE FUNCTION queue_deleted_session_photo_objects()
RETURNS trigger AS $$
BEGIN
    INSERT INTO photo_storage_deletions (id, storage_scope, storage_key)
    VALUES (gen_random_uuid(), OLD.storage_scope, OLD.storage_key_original)
    ON CONFLICT (storage_scope, storage_key) DO NOTHING;

    IF OLD.storage_key_thumbnail IS NOT NULL THEN
        INSERT INTO photo_storage_deletions (id, storage_scope, storage_key)
        VALUES (gen_random_uuid(), OLD.storage_scope, OLD.storage_key_thumbnail)
        ON CONFLICT (storage_scope, storage_key) DO NOTHING;
    END IF;

    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_queue_deleted_session_photo_objects
AFTER DELETE ON session_photos
FOR EACH ROW
EXECUTE FUNCTION queue_deleted_session_photo_objects();
