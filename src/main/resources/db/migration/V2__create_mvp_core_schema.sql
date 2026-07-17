CREATE TABLE hobby_categories (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT uk_hobby_categories_name UNIQUE (name)
);

CREATE TABLE hobbies (
    id UUID PRIMARY KEY,
    category_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    icon VARCHAR(100),
    default_equipment_category VARCHAR(100),
    CONSTRAINT fk_hobbies_category
        FOREIGN KEY (category_id) REFERENCES hobby_categories (id),
    CONSTRAINT uk_hobbies_name UNIQUE (name)
);

CREATE TABLE user_hobbies (
    user_id VARCHAR(128) NOT NULL,
    hobby_id UUID NOT NULL,
    experience_level VARCHAR(50),
    CONSTRAINT pk_user_hobbies PRIMARY KEY (user_id, hobby_id),
    CONSTRAINT fk_user_hobbies_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_hobbies_hobby
        FOREIGN KEY (hobby_id) REFERENCES hobbies (id)
);

CREATE TABLE hobby_attribute_template (
    id UUID PRIMARY KEY,
    hobby_id UUID NOT NULL,
    key VARCHAR(100) NOT NULL,
    label VARCHAR(150) NOT NULL,
    type VARCHAR(30) NOT NULL,
    unit VARCHAR(50),
    display_order INT NOT NULL,
    CONSTRAINT fk_hobby_attribute_template_hobby
        FOREIGN KEY (hobby_id) REFERENCES hobbies (id),
    CONSTRAINT uk_hobby_attribute_template_hobby_key UNIQUE (hobby_id, key),
    CONSTRAINT uk_hobby_attribute_template_hobby_order UNIQUE (hobby_id, display_order)
);

CREATE TABLE places (
    place_id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    lat NUMERIC(9, 6) NOT NULL,
    lng NUMERIC(9, 6) NOT NULL
);

CREATE TABLE equipment (
    id UUID PRIMARY KEY,
    user_id VARCHAR(128) NOT NULL,
    hobby_id UUID,
    category VARCHAR(100) NOT NULL,
    name VARCHAR(150) NOT NULL,
    CONSTRAINT fk_equipment_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_equipment_hobby
        FOREIGN KEY (hobby_id) REFERENCES hobbies (id)
);

CREATE TABLE backlog_items (
    id UUID PRIMARY KEY,
    user_id VARCHAR(128) NOT NULL,
    hobby_id UUID,
    title VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_backlog_items_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_backlog_items_hobby
        FOREIGN KEY (hobby_id) REFERENCES hobbies (id),
    CONSTRAINT ck_backlog_items_status
        CHECK (status IN ('pending', 'in_progress', 'done'))
);

CREATE TABLE sessions (
    id UUID PRIMARY KEY,
    user_id VARCHAR(128) NOT NULL,
    hobby_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    duration_minutes INT NOT NULL,
    notes TEXT,
    satisfaction INT NOT NULL,
    place_id VARCHAR(255),
    project_id UUID,
    attributes JSONB,
    CONSTRAINT fk_sessions_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_sessions_hobby
        FOREIGN KEY (hobby_id) REFERENCES hobbies (id),
    CONSTRAINT fk_sessions_place
        FOREIGN KEY (place_id) REFERENCES places (place_id),
    CONSTRAINT fk_sessions_project
        FOREIGN KEY (project_id) REFERENCES backlog_items (id),
    CONSTRAINT ck_sessions_duration_minutes
        CHECK (duration_minutes > 0),
    CONSTRAINT ck_sessions_satisfaction
        CHECK (satisfaction BETWEEN 1 AND 5)
);

CREATE TABLE session_photos (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL,
    storage_key_original VARCHAR(500) NOT NULL,
    storage_key_thumbnail VARCHAR(500) NOT NULL,
    CONSTRAINT fk_session_photos_session
        FOREIGN KEY (session_id) REFERENCES sessions (id)
);

CREATE TABLE session_equipment (
    session_id UUID NOT NULL,
    equipment_id UUID NOT NULL,
    CONSTRAINT pk_session_equipment PRIMARY KEY (session_id, equipment_id),
    CONSTRAINT fk_session_equipment_session
        FOREIGN KEY (session_id) REFERENCES sessions (id),
    CONSTRAINT fk_session_equipment_equipment
        FOREIGN KEY (equipment_id) REFERENCES equipment (id)
);

CREATE TABLE hobby_suggestions (
    id UUID PRIMARY KEY,
    user_id VARCHAR(128) NOT NULL,
    suggested_name VARCHAR(150) NOT NULL,
    status VARCHAR(30) NOT NULL,
    resulting_hobby_id UUID,
    CONSTRAINT fk_hobby_suggestions_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_hobby_suggestions_resulting_hobby
        FOREIGN KEY (resulting_hobby_id) REFERENCES hobbies (id),
    CONSTRAINT ck_hobby_suggestions_status
        CHECK (status IN ('pending', 'approved', 'rejected'))
);

CREATE INDEX idx_hobbies_category_id ON hobbies (category_id);
CREATE INDEX idx_user_hobbies_hobby_id ON user_hobbies (hobby_id);
CREATE INDEX idx_hobby_attribute_template_hobby_id ON hobby_attribute_template (hobby_id);
CREATE INDEX idx_equipment_user_id ON equipment (user_id);
CREATE INDEX idx_equipment_hobby_id ON equipment (hobby_id);
CREATE INDEX idx_backlog_items_user_id ON backlog_items (user_id);
CREATE INDEX idx_backlog_items_hobby_id ON backlog_items (hobby_id);
CREATE INDEX idx_sessions_user_id ON sessions (user_id);
CREATE INDEX idx_sessions_hobby_id ON sessions (hobby_id);
CREATE INDEX idx_sessions_started_at ON sessions (started_at);
CREATE INDEX idx_sessions_place_id ON sessions (place_id);
CREATE INDEX idx_sessions_project_id ON sessions (project_id);
CREATE INDEX idx_session_photos_session_id ON session_photos (session_id);
CREATE INDEX idx_hobby_suggestions_user_id ON hobby_suggestions (user_id);
CREATE INDEX idx_hobby_suggestions_resulting_hobby_id ON hobby_suggestions (resulting_hobby_id);
