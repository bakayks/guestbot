package com.guestbot.api.controller;

import com.guestbot.service.auth.AuthService;
import com.guestbot.service.auth.AuthService.TokenPair;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<TokenPair> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(
            authService.register(request.email(), request.password(),
                request.name(), request.phone())
        );
    }

    @PostMapping("/login")
    public ResponseEntity<TokenPair> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(
            authService.login(request.email(), request.password())
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenPair> refresh(@RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    public record RegisterRequest(
        @Email @NotBlank String email,
        @Size(min = 8) @NotBlank String password,
        @NotBlank String name,
        @NotBlank String phone
    ) {}

    public record LoginRequest(
        @Email @NotBlank String email,
        @NotBlank String password
    ) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}
}
