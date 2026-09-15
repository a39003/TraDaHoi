package com.trasua.api.dto;

import java.time.LocalDate;

public record AttendanceResponse(
        Long id,
        Long expenseId,
        LocalDate date,
        MemberBriefResponse member,
        MemberBriefResponse payer,
        String drinkName,
        long amount,
        int quantity
) {
}
