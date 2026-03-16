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
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request.email(), request.password(), request.name(), request.phone());
        return ResponseEntity.ok(new MessageResponse("Verification code sent to " + request.email()));
    }

    @PostMapping("/verify")
    public ResponseEntity<TokenPair> verify(@Valid @RequestBody VerifyRequest request) {
        return ResponseEntity.ok(authService.verify(request.email(), request.code()));
    }

    @PostMapping("/resend-code")
    public ResponseEntity<MessageResponse> resendCode(@Valid @RequestBody ResendRequest request) {
        authService.resendCode(request.email());
        return ResponseEntity.ok(new MessageResponse("Verification code resent to " + request.email()));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenPair> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request.email(), request.password()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenPair> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    public record RegisterRequest(
        @Email @NotBlank String email,
        @Size(min = 8) @NotBlank String password,
        @NotBlank String name,
        @NotBlank String phone
    ) {}

    public record VerifyRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 6, max = 6) String code
    ) {}

    public record ResendRequest(@Email @NotBlank String email) {}

    public record LoginRequest(
        @Email @NotBlank String email,
        @NotBlank String password
    ) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record MessageResponse(String message) {}
}
