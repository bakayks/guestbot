package com.guestbot.api.dto.response;

import com.guestbot.core.enums.BookingStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Данные бронирования")
public record BookingResponse(

    @Schema(description = "ID", example = "10") Long id,
    @Schema(description = "Дата создания") LocalDateTime createdAt,
    @Schema(description = "Номер бронирования", example = "BK-20260315-001") String bookingNumber,
    @Schema(description = "ID отеля", example = "1") Long hotelId,
    @Schema(description = "Название отеля", example = "Grand Hotel Tashkent") String hotelName,
    @Schema(description = "ID номера", example = "3") Long roomId,
    @Schema(description = "Тип номера", example = "Стандарт") String roomType,
    @Schema(description = "Имя гостя", example = "Иван Иванов") String guestName,
    @Schema(description = "Телефон гостя", example = "+998901234567") String guestPhone,
    @Schema(description = "Email гостя", example = "guest@example.com") String guestEmail,
    @Schema(description = "Дата заезда", example = "2026-05-01") LocalDate checkIn,
    @Schema(description = "Дата выезда", example = "2026-05-05") LocalDate checkOut,
    @Schema(description = "Количество ночей", example = "4") Integer nights,
    @Schema(description = "Итоговая сумма (сум)", example = "1400000.00") BigDecimal totalAmount,
    @Schema(description = "Статус", example = "PENDING_PAYMENT") BookingStatus status,
    @Schema(description = "Дедлайн оплаты") LocalDateTime paymentDeadline,
    @Schema(description = "Telegram chat ID гостя", example = "123456789") Long telegramChatId,
    @Schema(description = "Источник бронирования", example = "TELEGRAM") String source
) {}
