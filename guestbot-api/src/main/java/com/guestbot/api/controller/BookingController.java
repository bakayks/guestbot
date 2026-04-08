package com.guestbot.api.controller;

import com.guestbot.api.dto.request.CreateBookingRequest;
import com.guestbot.api.dto.response.BookingResponse;
import com.guestbot.api.dto.response.RoomAvailabilityResponse;
import com.guestbot.api.mapper.BookingMapper;
import com.guestbot.core.enums.CancellationReason;
import com.guestbot.service.booking.BookingService;
import com.guestbot.service.calendar.CalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Бронирования", description = "Управление бронированиями и проверка доступности номеров")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final CalendarService calendarService;
    private final BookingMapper bookingMapper;

    @Operation(summary = "Проверить доступность номеров", description = "Возвращает список номеров с количеством свободных мест на указанный период")
    @ApiResponse(responseCode = "200", description = "Список доступности")
    @ApiResponse(responseCode = "404", description = "Отель не найден")
    @GetMapping("/hotels/{hotelId}/availability")
    public ResponseEntity<List<RoomAvailabilityResponse>> checkAvailability(
        @Parameter(description = "ID отеля", example = "1") @PathVariable Long hotelId,
        @Parameter(description = "Дата заезда (YYYY-MM-DD)", example = "2026-05-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
        @Parameter(description = "Дата выезда (YYYY-MM-DD)", example = "2026-05-05") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut
    ) {
        return ResponseEntity.ok(bookingMapper.toAvailabilityList(
            calendarService.checkAvailability(hotelId, checkIn, checkOut)
        ));
    }

    @Operation(summary = "Создать бронирование")
    @ApiResponse(responseCode = "200", description = "Бронирование создано")
    @ApiResponse(responseCode = "400", description = "Номер недоступен или некорректные данные")
    @PostMapping("/bookings")
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody CreateBookingRequest request) {
        return ResponseEntity.ok(bookingMapper.toResponse(bookingService.create(
            request.hotelId(), request.roomId(),
            request.guestName(), request.guestPhone(), request.guestEmail(),
            request.checkIn(), request.checkOut(),
            request.telegramChatId()
        )));
    }

    @Operation(summary = "Получить бронирование по ID")
    @ApiResponse(responseCode = "200", description = "Бронирование найдено")
    @ApiResponse(responseCode = "404", description = "Бронирование не найдено")
    @GetMapping("/bookings/{id}")
    public ResponseEntity<BookingResponse> getById(
        @Parameter(description = "ID бронирования", example = "1") @PathVariable Long id
    ) {
        return ResponseEntity.ok(bookingMapper.toResponse(bookingService.getById(id)));
    }

    @Operation(summary = "Отменить бронирование", description = "Отменяет бронирование по инициативе гостя")
    @ApiResponse(responseCode = "200", description = "Бронирование отменено")
    @ApiResponse(responseCode = "404", description = "Бронирование не найдено")
    @PostMapping("/bookings/{id}/cancel")
    public ResponseEntity<BookingResponse> cancel(
        @Parameter(description = "ID бронирования", example = "1") @PathVariable Long id
    ) {
        return ResponseEntity.ok(bookingMapper.toResponse(
            bookingService.cancel(id, CancellationReason.GUEST_CANCELLED)
        ));
    }
}
