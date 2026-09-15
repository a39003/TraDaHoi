package com.trasua.api;

import com.trasua.api.dto.NotificationResponse;
import com.trasua.service.NotificationService;
import com.trasua.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notifications;
    private final AuthService auth;

    public NotificationController(NotificationService notifications, AuthService auth) {
        this.notifications = notifications;
        this.auth = auth;
    }

    @GetMapping
    public List<NotificationResponse> list(@RequestHeader("Authorization") String authorization) {
        return notifications.listForMember(auth.currentMember(authorization).getId());
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markRead(@RequestHeader("Authorization") String authorization,
                                         @PathVariable Long id) {
        return notifications.markRead(id, auth.currentMember(authorization).getId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader("Authorization") String authorization,
                       @PathVariable Long id) {
        notifications.delete(id, auth.currentMember(authorization).getId());
    }
}
