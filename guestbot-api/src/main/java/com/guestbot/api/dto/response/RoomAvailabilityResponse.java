package com.guestbot.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Доступность номера на указанный период")
public record RoomAvailabilityResponse(

    @Schema(description = "Данные номера") RoomResponse room,
    @Schema(description = "Количество свободных мест", example = "3") long availableCount,
    @Schema(description = "Дата заезда", example = "2026-05-01") LocalDate checkIn,
    @Schema(description = "Дата выезда", example = "2026-05-05") LocalDate checkOut,
    @Schema(description = "Количество ночей", example = "4") int nights
) {}
