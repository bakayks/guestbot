package com.guestbot.api.controller;

import com.guestbot.api.dto.request.RoomRequest;
import com.guestbot.api.dto.response.RoomResponse;
import com.guestbot.api.mapper.RoomMapper;
import com.guestbot.service.room.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Номера", description = "CRUD управление номерами отеля")
@RestController
@RequestMapping("/api/v1/hotels/{hotelId}/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final RoomMapper roomMapper;

    @Operation(summary = "Список номеров отеля")
    @ApiResponse(responseCode = "200", description = "Список номеров")
    @ApiResponse(responseCode = "404", description = "Отель не найден")
    @GetMapping
    public ResponseEntity<List<RoomResponse>> getByHotel(
        @Parameter(description = "ID отеля", example = "1") @PathVariable Long hotelId
    ) {
        return ResponseEntity.ok(roomMapper.toList(roomService.getByHotel(hotelId)));
    }

    @Operation(summary = "Получить номер по ID")
    @ApiResponse(responseCode = "200", description = "Номер найден")
    @ApiResponse(responseCode = "404", description = "Номер не найден")
    @GetMapping("/{id}")
    public ResponseEntity<RoomResponse> getById(
        @Parameter(description = "ID отеля", example = "1") @PathVariable Long hotelId,
        @Parameter(description = "ID номера", example = "3") @PathVariable Long id
    ) {
        return ResponseEntity.ok(roomMapper.toResponse(roomService.getById(id)));
    }

    @Operation(summary = "Создать номер")
    @ApiResponse(responseCode = "200", description = "Номер создан")
    @ApiResponse(responseCode = "404", description = "Отель не найден")
    @PostMapping
    public ResponseEntity<RoomResponse> create(
        @Parameter(description = "ID отеля", example = "1") @PathVariable Long hotelId,
        @RequestBody RoomRequest request
    ) {
        return ResponseEntity.ok(roomMapper.toResponse(
            roomService.create(hotelId, roomMapper.toEntity(request))
        ));
    }

    @Operation(summary = "Обновить номер")
    @ApiResponse(responseCode = "200", description = "Номер обновлён")
    @ApiResponse(responseCode = "404", description = "Номер не найден")
    @PutMapping("/{id}")
    public ResponseEntity<RoomResponse> update(
        @Parameter(description = "ID отеля", example = "1") @PathVariable Long hotelId,
        @Parameter(description = "ID номера", example = "3") @PathVariable Long id,
        @RequestBody RoomRequest request
    ) {
        return ResponseEntity.ok(roomMapper.toResponse(
            roomService.update(id, roomMapper.toEntity(request))
        ));
    }

    @Operation(summary = "Удалить номер")
    @ApiResponse(responseCode = "204", description = "Номер удалён")
    @ApiResponse(responseCode = "404", description = "Номер не найден")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @Parameter(description = "ID отеля", example = "1") @PathVariable Long hotelId,
        @Parameter(description = "ID номера", example = "3") @PathVariable Long id
    ) {
        roomService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
