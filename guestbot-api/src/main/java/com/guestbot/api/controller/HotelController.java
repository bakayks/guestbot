package com.guestbot.api.controller;

import com.guestbot.core.entity.Hotel;
import com.guestbot.service.hotel.HotelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hotels")
@RequiredArgsConstructor
public class HotelController {

    private final HotelService hotelService;

    @GetMapping
    public ResponseEntity<List<Hotel>> getMyHotels(@AuthenticationPrincipal Long ownerId) {
        return ResponseEntity.ok(hotelService.getByOwner(ownerId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Hotel> getById(@PathVariable Long id) {
        return ResponseEntity.ok(hotelService.getById(id));
    }

    @PostMapping
    public ResponseEntity<Hotel> create(
        @AuthenticationPrincipal Long ownerId,
        @RequestBody Hotel hotel
    ) {
        return ResponseEntity.ok(hotelService.create(ownerId, hotel));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Hotel> update(
        @PathVariable Long id,
        @AuthenticationPrincipal Long ownerId,
        @RequestBody Hotel hotel
    ) {
        return ResponseEntity.ok(hotelService.update(id, ownerId, hotel));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @PathVariable Long id,
        @AuthenticationPrincipal Long ownerId
    ) {
        hotelService.delete(id, ownerId);
        return ResponseEntity.noContent().build();
    }
}
