package com.trasua.api.dto;

import java.time.Instant;

public record ChatReactionResponse(Long id, MemberBriefResponse member, String emoji, Instant createdAt) {
}
