package com.trasua.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ExpenseResponse(
        Long id,
        LocalDate orderDate,
        MemberBriefResponse payer,
        MemberBriefResponse createdBy,
        long totalAmount,
        String splitMode,
        String note,
        List<ExpenseItemResponse> items,
        Instant createdAt
) {
}
