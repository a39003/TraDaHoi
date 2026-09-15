package com.trasua.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ExpenseItemRequest(
        @NotNull(message = "Hãy chọn thành viên uống nước") Long consumerMemberId,
        @NotBlank(message = "Tên đồ uống không được để trống") @Size(max = 150, message = "Tên đồ uống tối đa 150 ký tự") String drinkName,
        @PositiveOrZero(message = "Giá đồ uống không được là số âm") long unitPrice,
        @Positive(message = "Số lượng phải lớn hơn 0") int quantity
) {
}
