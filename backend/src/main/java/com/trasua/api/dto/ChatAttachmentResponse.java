package com.trasua.api.dto;

public record ChatAttachmentResponse(
        Long id,
        String url,
        String filename,
        String contentType,
        long sizeBytes
) {
}
