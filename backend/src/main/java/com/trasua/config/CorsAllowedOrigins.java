package com.trasua.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Parses the comma-separated CORS_ALLOWED_ORIGINS environment variable once
 * and shares the exact same trusted origins between HTTP and WebSocket APIs.
 */
@Component
public class CorsAllowedOrigins {
    private final String[] values;

    public CorsAllowedOrigins(@Value("${app.cors.allowed-origins}") String configuredOrigins) {
        this.values = Arrays.stream(configuredOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .distinct()
                .toArray(String[]::new);

        if (values.length == 0) {
            throw new IllegalStateException("CORS_ALLOWED_ORIGINS must contain at least one trusted frontend origin.");
        }
    }

    public String[] values() {
        return Arrays.copyOf(values, values.length);
    }
}
