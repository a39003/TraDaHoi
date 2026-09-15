package com.trasua.api.dto;

public record ChatUnreadResponse(long unreadCount, Long lastReadMessageId) {
}
