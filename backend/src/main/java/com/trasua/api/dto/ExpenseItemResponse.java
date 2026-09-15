package com.trasua.api.dto;

public record ExpenseItemResponse(
        Long id,
        MemberBriefResponse consumer,
        String drinkName,
        long unitPrice,
        int quantity,
        long lineTotal
) {
}
