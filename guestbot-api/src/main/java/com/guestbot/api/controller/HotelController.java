package com.guestbot.api.controller;

import com.guestbot.api.dto.request.HotelRequest;
import com.guestbot.api.dto.response.HotelResponse;
import com.guestbot.api.mapper.HotelMapper;
import com.guestbot.service.hotel.HotelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Отели", description = "CRUD управление отелями владельца")
@RestController
@RequestMapping("/api/v1/hotels")
@RequiredArgsConstructor
public class HotelController {

    private final HotelService hotelService;
    private final HotelMapper hotelMapper;

    @Operation(summary = "Мои отели", description = "Возвращает все отели текущего авторизованного владельца")
    @ApiResponse(responseCode = "200", description = "Список отелей")
    @GetMapping
    public ResponseEntity<List<HotelResponse>> getMyHotels(@AuthenticationPrincipal Long ownerId) {
        return ResponseEntity.ok(hotelMapper.toList(hotelService.getByOwner(ownerId)));
    }

    @Operation(summary = "Получить отель по ID")
    @ApiResponse(responseCode = "200", description = "Отель найден")
    @ApiResponse(responseCode = "404", description = "Отель не найден")
    @GetMapping("/{id}")
    public ResponseEntity<HotelResponse> getById(
        @Parameter(description = "ID отеля", example = "1") @PathVariable Long id
    ) {
        return ResponseEntity.ok(hotelMapper.toResponse(hotelService.getById(id)));
    }

    @Operation(summary = "Создать отель")
    @ApiResponse(responseCode = "200", description = "Отель создан")
    @PostMapping
    public ResponseEntity<HotelResponse> create(
        @AuthenticationPrincipal Long ownerId,
        @RequestBody HotelRequest request
    ) {
        return ResponseEntity.ok(hotelMapper.toResponse(
            hotelService.create(ownerId, hotelMapper.toEntity(request))
        ));
    }

    @Operation(summary = "Обновить отель")
    @ApiResponse(responseCode = "200", description = "Отель обновлён")
    @ApiResponse(responseCode = "403", description = "Нет доступа к этому отелю")
    @ApiResponse(responseCode = "404", description = "Отель не найден")
    @PutMapping("/{id}")
    public ResponseEntity<HotelResponse> update(
        @Parameter(description = "ID отеля", example = "1") @PathVariable Long id,
        @AuthenticationPrincipal Long ownerId,
        @RequestBody HotelRequest request
    ) {
        return ResponseEntity.ok(hotelMapper.toResponse(
            hotelService.update(id, ownerId, hotelMapper.toEntity(request))
        ));
    }

    @Operation(summary = "Удалить отель")
    @ApiResponse(responseCode = "204", description = "Отель удалён")
    @ApiResponse(responseCode = "403", description = "Нет доступа к этому отелю")
    @ApiResponse(responseCode = "404", description = "Отель не найден")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @Parameter(description = "ID отеля", example = "1") @PathVariable Long id,
        @AuthenticationPrincipal Long ownerId
    ) {
        hotelService.delete(id, ownerId);
        return ResponseEntity.noContent().build();
    }
}
