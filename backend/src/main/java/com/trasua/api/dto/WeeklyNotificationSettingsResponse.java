package com.trasua.api.dto;

import java.time.Instant;

public record WeeklyNotificationSettingsResponse(
        int sendDay,
        String sendTime,
        String debtorTitle,
        String debtorBody,
        String creditorTitle,
        String creditorBody,
        Instant updatedAt
) {
}
