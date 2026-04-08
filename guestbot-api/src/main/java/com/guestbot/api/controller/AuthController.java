package com.guestbot.api.controller;

import com.guestbot.service.auth.AuthService;
import com.guestbot.service.auth.AuthService.TokenPair;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Авторизация", description = "Регистрация, верификация и управление токенами")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Регистрация", description = "Отправляет код подтверждения на email", security = {})
    @ApiResponse(responseCode = "200", description = "Код отправлен")
    @ApiResponse(responseCode = "400", description = "Некорректные данные")
    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request.email(), request.password(), request.name(), request.phone());
        return ResponseEntity.ok(new MessageResponse("Verification code sent to " + request.email()));
    }

    @Operation(summary = "Верификация email", description = "Подтверждает код и возвращает пару токенов", security = {})
    @ApiResponse(responseCode = "200", description = "Токены выданы")
    @ApiResponse(responseCode = "400", description = "Неверный или просроченный код")
    @PostMapping("/verify")
    public ResponseEntity<TokenPair> verify(@Valid @RequestBody VerifyRequest request) {
        return ResponseEntity.ok(authService.verify(request.email(), request.code()));
    }

    @Operation(summary = "Повторная отправка кода", security = {})
    @ApiResponse(responseCode = "200", description = "Код отправлен повторно")
    @PostMapping("/resend-code")
    public ResponseEntity<MessageResponse> resendCode(@Valid @RequestBody ResendRequest request) {
        authService.resendCode(request.email());
        return ResponseEntity.ok(new MessageResponse("Verification code resent to " + request.email()));
    }

    @Operation(summary = "Вход", description = "Возвращает access и refresh токены", security = {})
    @ApiResponse(responseCode = "200", description = "Успешный вход")
    @ApiResponse(responseCode = "401", description = "Неверные учётные данные")
    @PostMapping("/login")
    public ResponseEntity<TokenPair> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request.email(), request.password()));
    }

    @Operation(summary = "Обновление токена", description = "Выдаёт новую пару токенов по refresh token", security = {})
    @ApiResponse(responseCode = "200", description = "Токены обновлены")
    @ApiResponse(responseCode = "401", description = "Refresh token недействителен или истёк")
    @PostMapping("/refresh")
    public ResponseEntity<TokenPair> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @Operation(summary = "Выход", description = "Инвалидирует refresh token")
    @ApiResponse(responseCode = "204", description = "Успешный выход")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @Schema(description = "Данные для регистрации")
    public record RegisterRequest(
        @Schema(description = "Email пользователя", example = "owner@hotel.uz") @Email @NotBlank String email,
        @Schema(description = "Пароль (минимум 8 символов)", example = "secret123") @Size(min = 8) @NotBlank String password,
        @Schema(description = "Имя владельца", example = "Алишер Каримов") @NotBlank String name,
        @Schema(description = "Телефон", example = "+998901234567") @NotBlank String phone
    ) {}

    @Schema(description = "Данные для верификации email")
    public record VerifyRequest(
        @Schema(description = "Email", example = "owner@hotel.uz") @Email @NotBlank String email,
        @Schema(description = "6-значный код из письма", example = "123456") @NotBlank @Size(min = 6, max = 6) String code
    ) {}

    @Schema(description = "Email для повторной отправки кода")
    public record ResendRequest(
        @Schema(description = "Email", example = "owner@hotel.uz") @Email @NotBlank String email
    ) {}

    @Schema(description = "Учётные данные для входа")
    public record LoginRequest(
        @Schema(description = "Email", example = "owner@hotel.uz") @Email @NotBlank String email,
        @Schema(description = "Пароль", example = "secret123") @NotBlank String password
    ) {}

    @Schema(description = "Refresh token")
    public record RefreshRequest(
        @Schema(description = "Refresh token", example = "eyJhbGciOiJIUzI1NiJ9...") @NotBlank String refreshToken
    ) {}

    @Schema(description = "Текстовый ответ")
    public record MessageResponse(
        @Schema(description = "Сообщение", example = "Verification code sent to owner@hotel.uz") String message
    ) {}
}
