ALTER TABLE session_photos
    ALTER COLUMN storage_key_thumbnail DROP NOT NULL,
    ADD COLUMN processing_status VARCHAR(20) NOT NULL DEFAULT 'pending',
    ADD COLUMN processing_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN last_processing_error VARCHAR(100);

ALTER TABLE session_photos
    ADD CONSTRAINT ck_session_photos_processing_status
        CHECK (processing_status IN ('pending', 'ready', 'failed'));

ALTER TABLE session_photos
    ADD CONSTRAINT ck_session_photos_processing_attempts
        CHECK (processing_attempts BETWEEN 0 AND 3);

CREATE INDEX idx_session_photos_processing_queue
    ON session_photos (processing_status, processing_attempts, id);
