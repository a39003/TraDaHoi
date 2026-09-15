package com.trasua.api.dto;

public record PaymentQrResponse(
        Long transferId,
        String qrImageUrl,
        String bankName,
        String accountNumber,
        String accountName,
        long amount,
        String transferContent
) {
}
