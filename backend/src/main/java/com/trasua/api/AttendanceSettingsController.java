package com.trasua.api;

import com.trasua.api.dto.AttendanceSettingsRequest;
import com.trasua.api.dto.AttendanceSettingsResponse;
import com.trasua.service.AttendanceSettingsService;
import com.trasua.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attendance-settings")
public class AttendanceSettingsController {
    private final AttendanceSettingsService settings;
    private final AuthService auth;

    public AttendanceSettingsController(AttendanceSettingsService settings, AuthService auth) {
        this.settings = settings;
        this.auth = auth;
    }

    @GetMapping
    public AttendanceSettingsResponse get(@RequestHeader("Authorization") String authorization) {
        auth.currentMember(authorization);
        return settings.get();
    }

    @PutMapping
    public AttendanceSettingsResponse update(@RequestHeader("Authorization") String authorization,
                                             @Valid @RequestBody AttendanceSettingsRequest request) {
        auth.requireAdmin(authorization);
        return settings.update(request);
    }
}
