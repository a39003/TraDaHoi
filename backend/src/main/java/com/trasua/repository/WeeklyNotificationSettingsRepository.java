package com.trasua.repository;

import com.trasua.domain.WeeklyNotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeeklyNotificationSettingsRepository extends JpaRepository<WeeklyNotificationSettings, Long> {
}
