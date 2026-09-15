package com.trasua.api.dto;

import com.trasua.domain.SettlementStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record SettlementResponse(
        Long id,
        LocalDate weekStart,
        LocalDate weekEnd,
        long totalAmount,
        SettlementStatus status,
        Instant finalizedAt,
        List<SettlementBalanceResponse> balances,
        List<TransferResponse> transfers
) {
}
