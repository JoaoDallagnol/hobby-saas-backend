CREATE INDEX idx_sessions_user_started_id
    ON sessions (user_id, started_at DESC, id DESC);

CREATE INDEX idx_sessions_user_hobby_started_id
    ON sessions (user_id, hobby_id, started_at DESC, id DESC);

CREATE INDEX idx_sessions_user_visibility_started_id
    ON sessions (user_id, visibility, started_at DESC, id DESC);

CREATE INDEX idx_sessions_user_hobby_visibility_started_id
    ON sessions (user_id, hobby_id, visibility, started_at DESC, id DESC);
