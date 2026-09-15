ALTER TABLE attendance_settings
    ADD COLUMN cutoff_time_text VARCHAR(5) NULL;

UPDATE attendance_settings
SET cutoff_time_text = DATE_FORMAT(cutoff_time, '%H:%i');

ALTER TABLE attendance_settings
    DROP COLUMN cutoff_time,
    CHANGE COLUMN cutoff_time_text cutoff_time VARCHAR(5) NOT NULL;
