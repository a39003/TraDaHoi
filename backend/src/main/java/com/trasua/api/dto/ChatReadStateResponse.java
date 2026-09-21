package com.trasua.api.dto;

public record ChatReadStateResponse(MemberBriefResponse member, Long lastReadMessageId) {
}
