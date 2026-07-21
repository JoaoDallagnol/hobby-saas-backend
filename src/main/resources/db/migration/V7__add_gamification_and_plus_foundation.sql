ALTER TABLE users
    ADD COLUMN profile_theme VARCHAR(30) NOT NULL DEFAULT 'default';

ALTER TABLE hobby_categories
    ADD COLUMN xp_session_bonus INT NOT NULL DEFAULT 10,
    ADD COLUMN xp_minutes_per_point INT NOT NULL DEFAULT 10,
    ADD CONSTRAINT ck_hobby_categories_xp_session_bonus CHECK (xp_session_bonus >= 0),
    ADD CONSTRAINT ck_hobby_categories_xp_minutes_per_point CHECK (xp_minutes_per_point > 0);

UPDATE hobby_categories SET xp_minutes_per_point = 5 WHERE name = 'Sports & Movement';
UPDATE hobby_categories SET xp_minutes_per_point = 8 WHERE name = 'Arts & Creativity';
UPDATE hobby_categories SET xp_minutes_per_point = 10 WHERE name = 'Learning & Intellectual';
UPDATE hobby_categories SET xp_minutes_per_point = 12 WHERE name = 'Games & Strategy';

CREATE TABLE subscriptions (
    user_id VARCHAR(128) PRIMARY KEY,
    plan VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    provider VARCHAR(50),
    external_subscription_id VARCHAR(255),
    current_period_end TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_subscriptions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_subscriptions_plan CHECK (plan IN ('plus')),
    CONSTRAINT ck_subscriptions_status CHECK (status IN ('active', 'past_due', 'canceled', 'expired'))
);

CREATE UNIQUE INDEX uk_subscriptions_provider_external_id
    ON subscriptions (provider, external_subscription_id)
    WHERE provider IS NOT NULL AND external_subscription_id IS NOT NULL;

CREATE TABLE hobby_xp (
    user_id VARCHAR(128) NOT NULL,
    hobby_id UUID NOT NULL,
    xp INT NOT NULL,
    level INT NOT NULL,
    level_label VARCHAR(50) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_hobby_xp PRIMARY KEY (user_id, hobby_id),
    CONSTRAINT fk_hobby_xp_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_hobby_xp_hobby FOREIGN KEY (hobby_id) REFERENCES hobbies (id),
    CONSTRAINT ck_hobby_xp_xp CHECK (xp >= 0),
    CONSTRAINT ck_hobby_xp_level CHECK (level BETWEEN 1 AND 5)
);

CREATE INDEX idx_hobby_xp_hobby_id ON hobby_xp (hobby_id);

CREATE TABLE goals (
    id UUID PRIMARY KEY,
    user_id VARCHAR(128) NOT NULL,
    hobby_id UUID,
    name VARCHAR(120) NOT NULL,
    metric VARCHAR(20) NOT NULL,
    target_value INT NOT NULL,
    cadence VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    advanced BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_goals_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_goals_hobby FOREIGN KEY (hobby_id) REFERENCES hobbies (id),
    CONSTRAINT ck_goals_metric CHECK (metric IN ('sessions', 'minutes')),
    CONSTRAINT ck_goals_target_value CHECK (target_value > 0),
    CONSTRAINT ck_goals_cadence CHECK (cadence IN ('weekly', 'monthly', 'custom')),
    CONSTRAINT ck_goals_status CHECK (status IN ('active', 'completed', 'archived')),
    CONSTRAINT ck_goals_period CHECK (end_date >= start_date)
);

CREATE INDEX idx_goals_user_status ON goals (user_id, status, start_date, end_date);
CREATE INDEX idx_goals_hobby_id ON goals (hobby_id);

CREATE TABLE user_badges (
    id UUID PRIMARY KEY,
    user_id VARCHAR(128) NOT NULL,
    badge_key VARCHAR(60) NOT NULL,
    hobby_id UUID,
    earned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_user_badges_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_badges_hobby FOREIGN KEY (hobby_id) REFERENCES hobbies (id)
);

CREATE UNIQUE INDEX uk_user_badges_scope
    ON user_badges (user_id, badge_key, COALESCE(hobby_id, '00000000-0000-0000-0000-000000000000'::uuid));
CREATE INDEX idx_user_badges_user_earned ON user_badges (user_id, earned_at DESC);

CREATE TABLE user_featured_badges (
    user_id VARCHAR(128) NOT NULL,
    badge_id UUID NOT NULL,
    position INT NOT NULL,
    CONSTRAINT pk_user_featured_badges PRIMARY KEY (user_id, position),
    CONSTRAINT uk_user_featured_badges_badge UNIQUE (user_id, badge_id),
    CONSTRAINT fk_user_featured_badges_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_featured_badges_badge FOREIGN KEY (badge_id) REFERENCES user_badges (id) ON DELETE CASCADE,
    CONSTRAINT ck_user_featured_badges_position CHECK (position BETWEEN 1 AND 3)
);

ALTER TABLE backlog_items
    ADD COLUMN due_date DATE,
    ADD COLUMN priority VARCHAR(20) NOT NULL DEFAULT 'normal',
    ADD COLUMN archived BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN position INT NOT NULL DEFAULT 0,
    ADD CONSTRAINT ck_backlog_items_priority CHECK (priority IN ('low', 'normal', 'high')),
    ADD CONSTRAINT ck_backlog_items_position CHECK (position >= 0);

CREATE INDEX idx_backlog_items_user_archived_status_position
    ON backlog_items (user_id, archived, status, position, created_at DESC);

CREATE TABLE equipment_maintenance_rules (
    id UUID PRIMARY KEY,
    equipment_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    interval_minutes INT NOT NULL,
    last_maintained_at TIMESTAMPTZ,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_equipment_maintenance_rules_equipment
        FOREIGN KEY (equipment_id) REFERENCES equipment (id) ON DELETE CASCADE,
    CONSTRAINT ck_equipment_maintenance_rules_interval CHECK (interval_minutes > 0)
);

CREATE INDEX idx_equipment_maintenance_rules_equipment
    ON equipment_maintenance_rules (equipment_id, active);
