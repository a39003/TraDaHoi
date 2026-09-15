package com.trasua.service;

import org.springframework.core.io.Resource;

public record ChatMediaContent(Resource resource, String contentType, long sizeBytes) {
}
