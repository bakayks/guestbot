package com.guestbot.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Запись базы знаний")
public record KnowledgeBaseResponse(

    @Schema(description = "ID", example = "5") Long id,
    @Schema(description = "Дата создания") LocalDateTime createdAt,
    @Schema(description = "Дата обновления") LocalDateTime updatedAt,
    @Schema(description = "ID отеля", example = "1") Long hotelId,
    @Schema(description = "Категория", example = "Питание") String category,
    @Schema(description = "Вопрос", example = "Есть ли завтрак?") String question,
    @Schema(description = "Ответ", example = "Да, завтрак включён и подаётся с 7:00 до 10:00.") String answer
) {}
