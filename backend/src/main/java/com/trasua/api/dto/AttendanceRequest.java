package com.trasua.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Quick single-person check-in. It creates one matching expense. */
public record AttendanceRequest(
        @NotNull LocalDate date,
        @NotNull Long memberId,
        @NotNull Long payerMemberId,
        @NotBlank @Size(max = 150) String drinkName,
        @PositiveOrZero long amount,
        @Size(max = 500) String note
) {
}
