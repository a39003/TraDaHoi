package com.trasua.api.dto;

import com.trasua.domain.TransferStatus;
import java.time.Instant;

public record TransferResponse(
        Long id,
        MemberResponse debtor,
        MemberResponse creditor,
        long amount,
        TransferStatus status,
        Instant paidAt,
        String transferContent,
        String qrImageUrl
) {
}
