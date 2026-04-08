package com.guestbot.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Данные для создания или обновления отеля")
public record HotelRequest(

    @Schema(description = "Название отеля", example = "Grand Hotel Tashkent", requiredMode = Schema.RequiredMode.REQUIRED)
    String name,

    @Schema(description = "Описание отеля", example = "Уютный отель в центре города")
    String description,

    @Schema(description = "Адрес", example = "ул. Амира Темура, 1")
    String address,

    @Schema(description = "Город", example = "Ташкент")
    String city,

    @Schema(description = "Телефон", example = "+998901234567")
    String phone,

    @Schema(description = "Email", example = "info@hotel.uz")
    String email,

    @Schema(description = "Сайт", example = "https://hotel.uz")
    String website,

    @Schema(description = "Широта", example = "41.2995")
    Double latitude,

    @Schema(description = "Долгота", example = "69.2401")
    Double longitude,

    @Schema(description = "Удобства (ключ — название, значение — наличие)", example = "{\"wifi\": true, \"parking\": false}")
    Map<String, Boolean> amenities,

    @Schema(description = "Время заезда", example = "14:00")
    String checkInTime,

    @Schema(description = "Время выезда", example = "12:00")
    String checkOutTime,

    @Schema(description = "Минимальный возраст гостя", example = "18")
    Integer minAge,

    @Schema(description = "Разрешено ли проживание с животными", example = "false")
    Boolean petsAllowed,

    @Schema(description = "Разрешено ли проживание с детьми", example = "true")
    Boolean childrenAllowed,

    @Schema(description = "Политика отмены бронирования")
    String cancellationPolicy,

    @Schema(description = "Название банка", example = "Uzpromstroybank")
    String bankName,

    @Schema(description = "Номер банковского счёта", example = "20208000200000000001")
    String bankAccount,

    @Schema(description = "ИНН / Tax ID", example = "123456789")
    String taxId,

    @Schema(description = "Получатель платежа", example = "ООО Grand Hotel")
    String bankRecipient,

    @Schema(description = "Активировать Telegram-бот", example = "true")
    Boolean botActive,

    @Schema(description = "Приветственное сообщение бота")
    String welcomeMessage,

    @Schema(description = "Сообщение вне рабочих часов")
    String offHoursMessage,

    @Schema(description = "Начало рабочего времени", example = "09:00")
    String workingHoursStart,

    @Schema(description = "Конец рабочего времени", example = "22:00")
    String workingHoursEnd
) {}
