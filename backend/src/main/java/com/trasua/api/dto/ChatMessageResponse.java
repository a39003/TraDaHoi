package com.trasua.api.dto;

import java.time.Instant;
import java.util.List;

public record ChatMessageResponse(
        Long id,
        MemberBriefResponse sender,
        String content,
        List<ChatAttachmentResponse> attachments,
        List<ChatReactionResponse> reactions,
        ChatReplyPreviewResponse replyTo,
        boolean deleted,
        Instant createdAt
) {
}
