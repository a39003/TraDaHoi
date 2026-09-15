CREATE TABLE chat_attachments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    message_id BIGINT NOT NULL,
    storage_key VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    byte_size BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_chat_attachments_message
        FOREIGN KEY (message_id) REFERENCES chat_messages(id) ON DELETE CASCADE,
    UNIQUE KEY uk_chat_attachments_storage_key (storage_key),
    KEY ix_chat_attachments_message (message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
