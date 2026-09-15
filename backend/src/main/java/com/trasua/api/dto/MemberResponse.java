package com.trasua.api.dto;

import java.time.Instant;

public record MemberResponse(
        Long id,
        String displayName,
        String email,
        String bankName,
        String bankBin,
        String accountNumber,
        String accountName,
        String qrCodeUrl,
        String avatarUrl,
        boolean active,
        String role,
        Instant createdAt
) {
}
