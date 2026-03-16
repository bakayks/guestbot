package com.guestbot.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guestbot.api.security.SecurityConfig;
import com.guestbot.core.exception.GuestBotException;
import com.guestbot.service.auth.AuthService;
import com.guestbot.service.auth.AuthService.TokenPair;
import com.guestbot.service.auth.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AuthService authService;
    @MockBean JwtService jwtService;

    private static final String BASE = "/api/v1/auth";

    // ─────────────────────────────────────────────
    // POST /register
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("register — успешная регистрация возвращает 200 и сообщение")
    void register_success() throws Exception {
        doNothing().when(authService).register(anyString(), anyString(), anyString(), anyString());

        mockMvc.perform(post(BASE + "/register").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "email", "owner@hotel.com",
                    "password", "secret123",
                    "name", "Иван Петров",
                    "phone", "+79001234567"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Verification code sent to owner@hotel.com"));

        verify(authService).register("owner@hotel.com", "secret123", "Иван Петров", "+79001234567");
    }

    @Test
    @DisplayName("register — невалидный email возвращает 400")
    void register_invalidEmail() throws Exception {
        mockMvc.perform(post(BASE + "/register").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "email", "not-an-email",
                    "password", "secret123",
                    "name", "Иван",
                    "phone", "+7900"
                ))))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("register — пароль короче 8 символов возвращает 400")
    void register_shortPassword() throws Exception {
        mockMvc.perform(post(BASE + "/register").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "email", "owner@hotel.com",
                    "password", "short",
                    "name", "Иван",
                    "phone", "+79001234567"
                ))))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("register — email уже занят возвращает 400")
    void register_emailAlreadyTaken() throws Exception {
        doThrow(new GuestBotException("Email already registered: owner@hotel.com"))
            .when(authService).register(anyString(), anyString(), anyString(), anyString());

        mockMvc.perform(post(BASE + "/register").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "email", "owner@hotel.com",
                    "password", "secret123",
                    "name", "Иван",
                    "phone", "+79001234567"
                ))))
            .andExpect(status().isBadRequest());
    }

    // ─────────────────────────────────────────────
    // POST /verify
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("verify — правильный код возвращает 200 и токены")
    void verify_success() throws Exception {
        TokenPair tokenPair = new TokenPair("access-token-abc", "refresh-token-xyz");
        when(authService.verify("owner@hotel.com", "123456")).thenReturn(tokenPair);

        mockMvc.perform(post(BASE + "/verify").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "email", "owner@hotel.com",
                    "code", "123456"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("access-token-abc"))
            .andExpect(jsonPath("$.refreshToken").value("refresh-token-xyz"));
    }

    @Test
    @DisplayName("verify — код не 6 символов возвращает 400")
    void verify_codeWrongLength() throws Exception {
        mockMvc.perform(post(BASE + "/verify").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "email", "owner@hotel.com",
                    "code", "12345"   // 5 символов — невалидно
                ))))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("verify — неверный код возвращает 400")
    void verify_wrongCode() throws Exception {
        when(authService.verify(anyString(), anyString()))
            .thenThrow(new GuestBotException("Invalid or expired verification code"));

        mockMvc.perform(post(BASE + "/verify").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "email", "owner@hotel.com",
                    "code", "000000"
                ))))
            .andExpect(status().isBadRequest());
    }

    // ─────────────────────────────────────────────
    // POST /resend-code
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("resendCode — успешная отправка возвращает 200 и сообщение")
    void resendCode_success() throws Exception {
        doNothing().when(authService).resendCode("owner@hotel.com");

        mockMvc.perform(post(BASE + "/resend-code").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", "owner@hotel.com"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Verification code resent to owner@hotel.com"));

        verify(authService).resendCode("owner@hotel.com");
    }

    @Test
    @DisplayName("resendCode — email уже верифицирован возвращает 400")
    void resendCode_alreadyVerified() throws Exception {
        doThrow(new GuestBotException("Email already verified"))
            .when(authService).resendCode("owner@hotel.com");

        mockMvc.perform(post(BASE + "/resend-code").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", "owner@hotel.com"))))
            .andExpect(status().isBadRequest());
    }

    // ─────────────────────────────────────────────
    // POST /login
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("login — успешный вход возвращает 200 и токены")
    void login_success() throws Exception {
        TokenPair tokenPair = new TokenPair("access-token", "refresh-token");
        when(authService.login("owner@hotel.com", "secret123")).thenReturn(tokenPair);

        mockMvc.perform(post(BASE + "/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "email", "owner@hotel.com",
                    "password", "secret123"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("access-token"))
            .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    @DisplayName("login — неверный пароль возвращает 400")
    void login_invalidCredentials() throws Exception {
        when(authService.login(anyString(), anyString()))
            .thenThrow(new GuestBotException("Invalid credentials"));

        mockMvc.perform(post(BASE + "/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "email", "owner@hotel.com",
                    "password", "wrongpassword"
                ))))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("login — email не верифицирован возвращает 400")
    void login_emailNotVerified() throws Exception {
        when(authService.login(anyString(), anyString()))
            .thenThrow(new GuestBotException("Email not verified. Please check your inbox."));

        mockMvc.perform(post(BASE + "/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "email", "owner@hotel.com",
                    "password", "secret123"
                ))))
            .andExpect(status().isBadRequest());
    }

    // ─────────────────────────────────────────────
    // POST /refresh
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("refresh — валидный refresh-token возвращает 200 и новые токены")
    void refresh_success() throws Exception {
        TokenPair tokenPair = new TokenPair("new-access", "new-refresh");
        when(authService.refresh("valid-refresh-token")).thenReturn(tokenPair);

        mockMvc.perform(post(BASE + "/refresh").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("refreshToken", "valid-refresh-token"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("new-access"))
            .andExpect(jsonPath("$.refreshToken").value("new-refresh"));
    }

    @Test
    @DisplayName("refresh — просроченный refresh-token возвращает 400")
    void refresh_expiredToken() throws Exception {
        when(authService.refresh(anyString()))
            .thenThrow(new GuestBotException("Refresh token expired"));

        mockMvc.perform(post(BASE + "/refresh").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("refreshToken", "expired-token"))))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("refresh — пустой refreshToken возвращает 400")
    void refresh_blankToken() throws Exception {
        mockMvc.perform(post(BASE + "/refresh").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("refreshToken", ""))))
            .andExpect(status().isBadRequest());
    }

    // ─────────────────────────────────────────────
    // POST /logout
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("logout — успешный выход возвращает 204")
    void logout_success() throws Exception {
        doNothing().when(authService).logout("valid-refresh-token");

        mockMvc.perform(post(BASE + "/logout").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("refreshToken", "valid-refresh-token"))))
            .andExpect(status().isNoContent());

        verify(authService).logout("valid-refresh-token");
    }

    @Test
    @DisplayName("logout — несуществующий токен всё равно возвращает 204 (idempotent)")
    void logout_unknownToken() throws Exception {
        doNothing().when(authService).logout(anyString());

        mockMvc.perform(post(BASE + "/logout").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("refreshToken", "unknown-token"))))
            .andExpect(status().isNoContent());
    }
}
