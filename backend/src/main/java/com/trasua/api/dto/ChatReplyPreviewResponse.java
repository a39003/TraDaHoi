package com.trasua.api.dto;

public record ChatReplyPreviewResponse(
        Long id,
        MemberBriefResponse sender,
        String content,
        boolean deleted
) {
}
