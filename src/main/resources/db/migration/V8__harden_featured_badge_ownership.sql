ALTER TABLE user_badges
    ADD CONSTRAINT uk_user_badges_user_id_id UNIQUE (user_id, id);

ALTER TABLE user_featured_badges
    DROP CONSTRAINT fk_user_featured_badges_badge,
    ADD CONSTRAINT fk_user_featured_badges_owned_badge
        FOREIGN KEY (user_id, badge_id)
        REFERENCES user_badges (user_id, id)
        ON DELETE CASCADE;
