ALTER TABLE weekly_notification_settings
    ADD COLUMN send_time_text VARCHAR(5) NULL;

UPDATE weekly_notification_settings
SET send_time_text = DATE_FORMAT(send_time, '%H:%i');

ALTER TABLE weekly_notification_settings
    DROP COLUMN send_time,
    CHANGE COLUMN send_time_text send_time VARCHAR(5) NOT NULL;
