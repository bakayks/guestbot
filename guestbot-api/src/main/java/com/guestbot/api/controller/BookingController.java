package com.guestbot.api.controller;

import com.guestbot.core.entity.Booking;
import com.guestbot.core.enums.CancellationReason;
import com.guestbot.service.booking.BookingService;
import com.guestbot.service.calendar.CalendarService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final CalendarService calendarService;

    @GetMapping("/hotels/{hotelId}/availability")
    public ResponseEntity<List<CalendarService.RoomAvailability>> checkAvailability(
        @PathVariable Long hotelId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut
    ) {
        return ResponseEntity.ok(calendarService.checkAvailability(hotelId, checkIn, checkOut));
    }

    @PostMapping("/bookings")
    public ResponseEntity<Booking> create(@RequestBody CreateBookingRequest request) {
        Booking booking = bookingService.create(
            request.hotelId(), request.roomId(),
            request.guestName(), request.guestPhone(), request.guestEmail(),
            request.checkIn(), request.checkOut(),
            request.telegramChatId()
        );
        return ResponseEntity.ok(booking);
    }

    @GetMapping("/bookings/{id}")
    public ResponseEntity<Booking> getById(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getById(id));
    }

    @PostMapping("/bookings/{id}/cancel")
    public ResponseEntity<Booking> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.cancel(id, CancellationReason.GUEST_CANCELLED));
    }

    public record CreateBookingRequest(
        @NotNull Long hotelId,
        @NotNull Long roomId,
        @NotNull String guestName,
        @NotNull String guestPhone,
        String guestEmail,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
        Long telegramChatId
    ) {}
}
