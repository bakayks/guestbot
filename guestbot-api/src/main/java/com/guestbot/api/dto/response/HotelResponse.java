package com.guestbot.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "Данные отеля")
public record HotelResponse(

    @Schema(description = "ID", example = "1") Long id,
    @Schema(description = "Дата создания") LocalDateTime createdAt,
    @Schema(description = "Дата обновления") LocalDateTime updatedAt,
    @Schema(description = "ID владельца", example = "5") Long ownerId,
    @Schema(description = "Название", example = "Grand Hotel Tashkent") String name,
    @Schema(description = "Описание") String description,
    @Schema(description = "Адрес") String address,
    @Schema(description = "Город") String city,
    @Schema(description = "Телефон") String phone,
    @Schema(description = "Email") String email,
    @Schema(description = "Сайт") String website,
    @Schema(description = "Широта") Double latitude,
    @Schema(description = "Долгота") Double longitude,
    @Schema(description = "Удобства") Map<String, Boolean> amenities,
    @Schema(description = "Время заезда", example = "14:00") String checkInTime,
    @Schema(description = "Время выезда", example = "12:00") String checkOutTime,
    @Schema(description = "Минимальный возраст") Integer minAge,
    @Schema(description = "Разрешены животные") Boolean petsAllowed,
    @Schema(description = "Разрешены дети") Boolean childrenAllowed,
    @Schema(description = "Политика отмены") String cancellationPolicy,
    @Schema(description = "Название банка") String bankName,
    @Schema(description = "Номер счёта") String bankAccount,
    @Schema(description = "ИНН") String taxId,
    @Schema(description = "Получатель платежа") String bankRecipient,
    @Schema(description = "Бот активен") Boolean botActive,
    @Schema(description = "Приветственное сообщение") String welcomeMessage,
    @Schema(description = "Сообщение вне рабочих часов") String offHoursMessage,
    @Schema(description = "Начало рабочего времени") String workingHoursStart,
    @Schema(description = "Конец рабочего времени") String workingHoursEnd,
    @Schema(description = "Активен") boolean active
) {}
