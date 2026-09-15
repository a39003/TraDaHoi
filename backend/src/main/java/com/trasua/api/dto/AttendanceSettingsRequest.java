package com.trasua.api.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record AttendanceSettingsRequest(
        @NotNull(message = "Hãy chọn giờ khóa điểm danh") LocalTime cutoffTime
) {
}
