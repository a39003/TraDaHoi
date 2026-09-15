ALTER TABLE drink_orders
    ADD COLUMN created_by_member_id BIGINT NULL AFTER payer_member_id,
    ADD COLUMN split_mode VARCHAR(20) NOT NULL DEFAULT 'ITEMIZED' AFTER total_amount;

UPDATE drink_orders SET created_by_member_id = payer_member_id WHERE created_by_member_id IS NULL;

ALTER TABLE drink_orders
    MODIFY created_by_member_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_orders_created_by FOREIGN KEY (created_by_member_id) REFERENCES members(id),
    ADD KEY ix_orders_created_by (created_by_member_id);

CREATE TABLE attendance_settings (
    id BIGINT NOT NULL,
    cutoff_time TIME NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO attendance_settings (id, cutoff_time, updated_at)
VALUES (1, '15:00:00', NOW(6));

CREATE TABLE member_favorite_drinks (
    member_id BIGINT NOT NULL,
    drink_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (member_id, drink_id),
    CONSTRAINT fk_favorite_member FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE,
    CONSTRAINT fk_favorite_drink FOREIGN KEY (drink_id) REFERENCES drink_catalog(id) ON DELETE CASCADE,
    KEY ix_favorite_drink (drink_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE chat_message_reactions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    message_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    emoji VARCHAR(32) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_reaction_message FOREIGN KEY (message_id) REFERENCES chat_messages(id) ON DELETE CASCADE,
    CONSTRAINT fk_reaction_member FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE,
    UNIQUE KEY uk_reaction_message_member_emoji (message_id, member_id, emoji),
    KEY ix_reaction_message (message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
