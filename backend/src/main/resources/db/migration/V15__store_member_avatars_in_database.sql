ALTER TABLE members
    ADD COLUMN avatar_data MEDIUMBLOB NULL,
    ADD COLUMN avatar_content_type VARCHAR(100) NULL;
