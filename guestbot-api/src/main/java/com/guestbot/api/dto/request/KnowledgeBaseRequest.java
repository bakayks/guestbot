package com.guestbot.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Данные для создания или обновления записи базы знаний")
public record KnowledgeBaseRequest(

    @Schema(description = "Категория", example = "Питание", requiredMode = Schema.RequiredMode.REQUIRED)
    String category,

    @Schema(description = "Вопрос гостя", example = "Есть ли завтрак?", requiredMode = Schema.RequiredMode.REQUIRED)
    String question,

    @Schema(description = "Ответ бота", example = "Да, завтрак включён и подаётся с 7:00 до 10:00.", requiredMode = Schema.RequiredMode.REQUIRED)
    String answer
) {}
