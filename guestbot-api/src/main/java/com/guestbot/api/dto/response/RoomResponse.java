package com.guestbot.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "Данные номера")
public record RoomResponse(

    @Schema(description = "ID", example = "3") Long id,
    @Schema(description = "Дата создания") LocalDateTime createdAt,
    @Schema(description = "Дата обновления") LocalDateTime updatedAt,
    @Schema(description = "ID отеля", example = "1") Long hotelId,
    @Schema(description = "Тип номера", example = "Стандарт") String type,
    @Schema(description = "Описание") String description,
    @Schema(description = "Вместимость", example = "2") Integer capacity,
    @Schema(description = "Кол-во номеров данного типа", example = "5") Integer count,
    @Schema(description = "Цена за ночь (сум)", example = "350000.00") BigDecimal pricePerNight,
    @Schema(description = "Удобства") Map<String, Boolean> amenities,
    @Schema(description = "Активен") boolean active
) {}
