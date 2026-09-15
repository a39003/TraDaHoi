package com.trasua.service;

import com.trasua.api.dto.AttendanceSettingsRequest;
import com.trasua.api.dto.AttendanceSettingsResponse;
import com.trasua.domain.AttendanceSettings;
import com.trasua.repository.AttendanceSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

@Service
public class AttendanceSettingsService {
    public static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private final AttendanceSettingsRepository settings;

    public AttendanceSettingsService(AttendanceSettingsRepository settings) {
        this.settings = settings;
    }

    @Transactional(readOnly = true)
    public AttendanceSettingsResponse get() {
        LocalTime cutoff = cutoffTime();
        LocalDate today = LocalDate.now(VIETNAM);
        LocalTime now = LocalTime.now(VIETNAM);
        return new AttendanceSettingsResponse(cutoff, today, now, !now.isBefore(cutoff));
    }

    @Transactional
    public AttendanceSettingsResponse update(AttendanceSettingsRequest request) {
        AttendanceSettings entity = settings.findById(1L).orElseGet(() -> {
            AttendanceSettings created = new AttendanceSettings();
            created.setId(1L);
            return created;
        });
        entity.setCutoffTime(format(request.cutoffTime()));
        entity.setUpdatedAt(Instant.now());
        settings.save(entity);
        return get();
    }

    @Transactional(readOnly = true)
    public LocalTime cutoffTime() {
        return settings.findById(1L).map(AttendanceSettings::getCutoffTime).map(LocalTime::parse).orElse(LocalTime.of(15, 0));
    }

    private String format(LocalTime value) {
        return String.format("%02d:%02d", value.getHour(), value.getMinute());
    }
}
