package com.trasua.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatReactionRequest(
        @NotBlank(message = "Hãy chọn cảm xúc") @Size(max = 16, message = "Cảm xúc không hợp lệ") String emoji
) {
}
