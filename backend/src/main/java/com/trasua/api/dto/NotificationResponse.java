package com.trasua.api.dto;

import com.trasua.domain.NotificationType;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String body,
        Long settlementId,
        Long transferId,
        Instant readAt,
        Instant createdAt
) {
}
