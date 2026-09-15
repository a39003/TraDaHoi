package com.trasua.api.dto;

import java.time.LocalDate;
import java.util.List;

public record DailySummaryResponse(
        LocalDate date,
        long totalAmount,
        int drinkCount,
        List<AttendanceResponse> attendances,
        List<ExpenseResponse> expenses
) {
}
