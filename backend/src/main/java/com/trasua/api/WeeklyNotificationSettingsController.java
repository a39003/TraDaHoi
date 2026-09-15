package com.trasua.api;

import com.trasua.api.dto.WeeklyNotificationSettingsRequest;
import com.trasua.api.dto.WeeklyNotificationSettingsResponse;
import com.trasua.service.AuthService;
import com.trasua.service.WeeklyNotificationSettingsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/weekly-notification-settings")
public class WeeklyNotificationSettingsController {
    private final WeeklyNotificationSettingsService settings;
    private final AuthService auth;

    public WeeklyNotificationSettingsController(WeeklyNotificationSettingsService settings, AuthService auth) {
        this.settings = settings;
        this.auth = auth;
    }

    @GetMapping
    public WeeklyNotificationSettingsResponse get(@RequestHeader("Authorization") String authorization) {
        auth.requireAdmin(authorization);
        return settings.get();
    }

    @PutMapping
    public WeeklyNotificationSettingsResponse update(@RequestHeader("Authorization") String authorization,
                                                     @Valid @RequestBody WeeklyNotificationSettingsRequest request) {
        auth.requireAdmin(authorization);
        return settings.update(request);
    }
}
