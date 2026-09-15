ALTER TABLE chat_messages
    ADD COLUMN reply_to_message_id BIGINT NULL,
    ADD COLUMN deleted_at DATETIME(6) NULL,
    ADD CONSTRAINT fk_chat_reply
        FOREIGN KEY (reply_to_message_id) REFERENCES chat_messages(id) ON DELETE SET NULL,
    ADD KEY ix_chat_reply (reply_to_message_id);

CREATE TABLE weekly_notification_settings (
    id BIGINT NOT NULL,
    send_day TINYINT NOT NULL,
    send_time TIME NOT NULL,
    debtor_title VARCHAR(200) NOT NULL,
    debtor_body VARCHAR(1000) NOT NULL,
    creditor_title VARCHAR(200) NOT NULL,
    creditor_body VARCHAR(1000) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO weekly_notification_settings (
    id, send_day, send_time, debtor_title, debtor_body, creditor_title, creditor_body, updated_at
) VALUES (
    1,
    6,
    '15:00:00',
    'Den luot thanh toan tra da',
    'Ban can chuyen {amount} cho {creditor} cho tuan {weekStart}.',
    'Tong hop tien tra da tuan nay',
    'Ban da tra truoc tien nuoc. Cac thanh vien se hoan lai tong {amount} cho ban trong tuan {weekStart}.',
    NOW(6)
);
