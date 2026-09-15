ALTER TABLE members
    ADD COLUMN last_chat_read_message_id BIGINT NULL;

UPDATE members
SET last_chat_read_message_id = (SELECT MAX(id) FROM chat_messages);
