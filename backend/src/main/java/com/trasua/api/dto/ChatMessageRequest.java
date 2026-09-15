package com.trasua.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChatMessageRequest(
        @NotNull(message = "Thiếu người gửi tin nhắn") Long senderMemberId,
        @NotBlank(message = "Tin nhắn không được để trống") @Size(max = 2000, message = "Tin nhắn tối đa 2.000 ký tự") String content,
        Long replyToMessageId
) {
}
