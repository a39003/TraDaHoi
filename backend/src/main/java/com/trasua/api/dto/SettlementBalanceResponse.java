package com.trasua.api.dto;

public record SettlementBalanceResponse(
        MemberResponse member,
        long consumedAmount,
        long paidAmount,
        long netAmount
) {
}
