package com.guestbot.api.controller;

import com.guestbot.api.dto.request.KnowledgeBaseRequest;
import com.guestbot.api.dto.response.KnowledgeBaseResponse;
import com.guestbot.api.mapper.KnowledgeBaseMapper;
import com.guestbot.service.knowledge.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "База знаний", description = "Управление FAQ-записями для Telegram-бота отеля")
@RestController
@RequestMapping("/api/v1/hotels/{hotelId}/knowledge-base")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final KnowledgeBaseMapper knowledgeBaseMapper;

    @Operation(summary = "Список записей базы знаний")
    @ApiResponse(responseCode = "200", description = "Список записей")
    @ApiResponse(responseCode = "404", description = "Отель не найден")
    @GetMapping
    public ResponseEntity<List<KnowledgeBaseResponse>> getAll(
        @Parameter(description = "ID отеля", example = "1") @PathVariable Long hotelId
    ) {
        return ResponseEntity.ok(knowledgeBaseMapper.toList(knowledgeBaseService.getByHotel(hotelId)));
    }

    @Operation(summary = "Создать запись", description = "Добавляет новую пару вопрос-ответ в базу знаний бота")
    @ApiResponse(responseCode = "200", description = "Запись создана")
    @ApiResponse(responseCode = "404", description = "Отель не найден")
    @PostMapping
    public ResponseEntity<KnowledgeBaseResponse> create(
        @Parameter(description = "ID отеля", example = "1") @PathVariable Long hotelId,
        @RequestBody KnowledgeBaseRequest request
    ) {
        return ResponseEntity.ok(knowledgeBaseMapper.toResponse(
            knowledgeBaseService.create(hotelId, knowledgeBaseMapper.toEntity(request))
        ));
    }

    @Operation(summary = "Обновить запись")
    @ApiResponse(responseCode = "200", description = "Запись обновлена")
    @ApiResponse(responseCode = "404", description = "Запись не найдена")
    @PutMapping("/{id}")
    public ResponseEntity<KnowledgeBaseResponse> update(
        @Parameter(description = "ID отеля", example = "1") @PathVariable Long hotelId,
        @Parameter(description = "ID записи", example = "5") @PathVariable Long id,
        @RequestBody KnowledgeBaseRequest request
    ) {
        return ResponseEntity.ok(knowledgeBaseMapper.toResponse(
            knowledgeBaseService.update(id, knowledgeBaseMapper.toEntity(request))
        ));
    }

    @Operation(summary = "Удалить запись")
    @ApiResponse(responseCode = "204", description = "Запись удалена")
    @ApiResponse(responseCode = "404", description = "Запись не найдена")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @Parameter(description = "ID отеля", example = "1") @PathVariable Long hotelId,
        @Parameter(description = "ID записи", example = "5") @PathVariable Long id
    ) {
        knowledgeBaseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
