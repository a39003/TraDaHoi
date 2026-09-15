package com.trasua.repository;

import com.trasua.domain.AttendanceSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceSettingsRepository extends JpaRepository<AttendanceSettings, Long> {
}
