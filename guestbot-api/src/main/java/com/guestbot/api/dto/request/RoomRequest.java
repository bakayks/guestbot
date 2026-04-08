package com.guestbot.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.Map;

@Schema(description = "Данные для создания или обновления номера")
public record RoomRequest(

    @Schema(description = "Тип номера", example = "Стандарт", requiredMode = Schema.RequiredMode.REQUIRED)
    String type,

    @Schema(description = "Описание номера", example = "Просторный номер с видом на горы")
    String description,

    @Schema(description = "Вместимость (кол-во гостей)", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    Integer capacity,

    @Schema(description = "Количество номеров данного типа", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    Integer count,

    @Schema(description = "Цена за ночь (сум)", example = "350000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    BigDecimal pricePerNight,

    @Schema(description = "Удобства (ключ — название, значение — наличие)", example = "{\"tv\": true, \"minibar\": false}")
    Map<String, Boolean> amenities
) {}
