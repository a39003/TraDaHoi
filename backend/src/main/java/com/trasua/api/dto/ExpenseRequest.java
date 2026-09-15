package com.trasua.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record ExpenseRequest(
        @NotNull(message = "Ngày uống nước không được để trống") LocalDate orderDate,
        @NotNull(message = "Hãy chọn người đã trả tiền") Long payerMemberId,
        @Size(max = 20, message = "Kiểu chia tiền không hợp lệ") String splitMode,
        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự") String note,
        @NotEmpty(message = "Phải có ít nhất một đồ uống") List<@Valid ExpenseItemRequest> items
) {
}
