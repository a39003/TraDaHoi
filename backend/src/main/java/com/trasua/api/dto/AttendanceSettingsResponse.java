package com.trasua.api.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record AttendanceSettingsResponse(
        LocalTime cutoffTime,
        LocalDate serverDate,
        LocalTime serverTime,
        boolean locked
) {
}
