package com.guestbot.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Schema(description = "Запрос на создание бронирования")
public record CreateBookingRequest(

    @Schema(description = "ID отеля", example = "1") @NotNull Long hotelId,
    @Schema(description = "ID номера", example = "3") @NotNull Long roomId,
    @Schema(description = "Имя гостя", example = "Иван Иванов") @NotNull String guestName,
    @Schema(description = "Телефон гостя", example = "+998901234567") @NotNull String guestPhone,
    @Schema(description = "Email гостя", example = "guest@example.com") String guestEmail,
    @Schema(description = "Дата заезда", example = "2026-05-01") @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
    @Schema(description = "Дата выезда", example = "2026-05-05") @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
    @Schema(description = "Telegram chat ID гостя для уведомлений", example = "123456789") Long telegramChatId
) {}
