package com.trasua.api;

import com.trasua.api.dto.AuthRequests;
import com.trasua.api.dto.AuthResponse;
import com.trasua.api.dto.ResetCodeResponse;
import com.trasua.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth = auth; }
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequests.Login request) { return auth.login(request); }
    @PostMapping("/forgot-password")
    public ResetCodeResponse forgotPassword(@Valid @RequestBody AuthRequests.RequestReset request) { return auth.requestReset(request); }
    @PostMapping("/reset-password") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody AuthRequests.ResetPassword request) { auth.resetPassword(request); }
}
