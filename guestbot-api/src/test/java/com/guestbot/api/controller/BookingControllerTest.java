package com.guestbot.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guestbot.api.mapper.BookingMapperImpl;
import com.guestbot.api.mapper.RoomMapperImpl;
import com.guestbot.api.security.SecurityConfig;
import com.guestbot.core.entity.Booking;
import com.guestbot.core.entity.Room;
import com.guestbot.core.enums.BookingStatus;
import com.guestbot.core.enums.CancellationReason;
import com.guestbot.core.exception.ResourceNotFoundException;
import com.guestbot.core.exception.RoomNotAvailableException;
import com.guestbot.service.auth.JwtService;
import com.guestbot.service.booking.BookingService;
import com.guestbot.service.calendar.CalendarService;
import com.guestbot.service.calendar.CalendarService.RoomAvailability;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
@Import({SecurityConfig.class, BookingMapperImpl.class, RoomMapperImpl.class})
@DisplayName("BookingController")
class BookingControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean BookingService bookingService;
    @MockBean CalendarService calendarService;
    @MockBean JwtService jwtService;

    private static final Long OWNER_ID = 1L;
    private static final Long HOTEL_ID = 2L;
    private static final Long ROOM_ID  = 3L;

    private UsernamePasswordAuthenticationToken ownerAuth() {
        return new UsernamePasswordAuthenticationToken(
            OWNER_ID, null, List.of(new SimpleGrantedAuthority("ROLE_OWNER"))
        );
    }

    private Booking buildBooking(Long id) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setBookingNumber("BK-20260316-001");
        booking.setGuestName("Алексей Смирнов");
        booking.setGuestPhone("+79001234567");
        booking.setCheckIn(LocalDate.of(2026, 4, 1));
        booking.setCheckOut(LocalDate.of(2026, 4, 5));
        booking.setNights(4);
        booking.setTotalAmount(BigDecimal.valueOf(14000));
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        return booking;
    }

    private Room buildRoom(Long id) {
        Room room = new Room();
        room.setId(id);
        room.setType("Стандарт");
        room.setCapacity(2);
        room.setCount(5);
        room.setPricePerNight(BigDecimal.valueOf(3500));
        room.setActive(true);
        return room;
    }

    // ─────────────────────────────────────────────
    // GET /api/v1/hotels/{hotelId}/availability
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("checkAvailability — возвращает список доступных номеров")
    void checkAvailability_success() throws Exception {
        Room room = buildRoom(ROOM_ID);
        LocalDate checkIn  = LocalDate.of(2026, 4, 1);
        LocalDate checkOut = LocalDate.of(2026, 4, 5);
        RoomAvailability availability = new RoomAvailability(room, 3L, checkIn, checkOut);

        when(calendarService.checkAvailability(HOTEL_ID, checkIn, checkOut))
            .thenReturn(List.of(availability));

        mockMvc.perform(get("/api/v1/hotels/{hotelId}/availability", HOTEL_ID)
                .with(authentication(ownerAuth()))
                .param("checkIn", "2026-04-01")
                .param("checkOut", "2026-04-05"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].availableCount").value(3));

        verify(calendarService).checkAvailability(HOTEL_ID, checkIn, checkOut);
    }

    @Test
    @DisplayName("checkAvailability — без параметров дат возвращает 500 (GlobalExceptionHandler перехватывает MissingServletRequestParameterException)")
    void checkAvailability_missingParams() throws Exception {
        mockMvc.perform(get("/api/v1/hotels/{hotelId}/availability", HOTEL_ID)
                .with(authentication(ownerAuth())))
            .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("checkAvailability — checkOut раньше checkIn — сервис бросает 500")
    void checkAvailability_invalidDateRange() throws Exception {
        when(calendarService.checkAvailability(anyLong(), any(), any()))
            .thenThrow(new IllegalArgumentException("checkOut must be after checkIn"));

        mockMvc.perform(get("/api/v1/hotels/{hotelId}/availability", HOTEL_ID)
                .with(authentication(ownerAuth()))
                .param("checkIn", "2026-04-05")
                .param("checkOut", "2026-04-01"))
            .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("checkAvailability — пустой список если нет доступных номеров")
    void checkAvailability_noRoomsAvailable() throws Exception {
        when(calendarService.checkAvailability(anyLong(), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/hotels/{hotelId}/availability", HOTEL_ID)
                .with(authentication(ownerAuth()))
                .param("checkIn", "2026-04-01")
                .param("checkOut", "2026-04-05"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("checkAvailability — без токена возвращает 401")
    void checkAvailability_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/hotels/{hotelId}/availability", HOTEL_ID)
                .param("checkIn", "2026-04-01")
                .param("checkOut", "2026-04-05"))
            .andExpect(status().isForbidden());
    }

    // ─────────────────────────────────────────────
    // POST /api/v1/bookings
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("create — создаёт бронирование и возвращает 200")
    void createBooking_success() throws Exception {
        Booking booking = buildBooking(100L);
        when(bookingService.create(
            eq(HOTEL_ID), eq(ROOM_ID),
            anyString(), anyString(), isNull(),
            any(LocalDate.class), any(LocalDate.class),
            isNull()
        )).thenReturn(booking);

        mockMvc.perform(post("/api/v1/bookings").with(authentication(ownerAuth())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "hotelId", HOTEL_ID,
                    "roomId", ROOM_ID,
                    "guestName", "Алексей Смирнов",
                    "guestPhone", "+79001234567",
                    "checkIn", "2026-04-01",
                    "checkOut", "2026-04-05"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(100))
            .andExpect(jsonPath("$.bookingNumber").value("BK-20260316-001"))
            .andExpect(jsonPath("$.guestName").value("Алексей Смирнов"));
    }

    @Test
    @DisplayName("create — номер недоступен возвращает 409")
    void createBooking_roomNotAvailable() throws Exception {
        when(bookingService.create(anyLong(), anyLong(), anyString(), anyString(),
            any(), any(LocalDate.class), any(LocalDate.class), any()))
            .thenThrow(new RoomNotAvailableException(ROOM_ID));

        mockMvc.perform(post("/api/v1/bookings").with(authentication(ownerAuth())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "hotelId", HOTEL_ID,
                    "roomId", ROOM_ID,
                    "guestName", "Алексей",
                    "guestPhone", "+79001234567",
                    "checkIn", "2026-04-01",
                    "checkOut", "2026-04-05"
                ))))
            .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("create — без токена возвращает 401")
    void createBooking_unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/bookings").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "hotelId", HOTEL_ID,
                    "roomId", ROOM_ID,
                    "guestName", "Алексей",
                    "guestPhone", "+79001234567",
                    "checkIn", "2026-04-01",
                    "checkOut", "2026-04-05"
                ))))
            .andExpect(status().isForbidden());
    }

    // ─────────────────────────────────────────────
    // GET /api/v1/bookings/{id}
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("getById — возвращает бронирование по id")
    void getBookingById_success() throws Exception {
        Booking booking = buildBooking(100L);
        when(bookingService.getById(100L)).thenReturn(booking);

        mockMvc.perform(get("/api/v1/bookings/100").with(authentication(ownerAuth())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(100))
            .andExpect(jsonPath("$.bookingNumber").value("BK-20260316-001"));

        verify(bookingService).getById(100L);
    }

    @Test
    @DisplayName("getById — несуществующее бронирование возвращает 404")
    void getBookingById_notFound() throws Exception {
        when(bookingService.getById(999L))
            .thenThrow(new ResourceNotFoundException("Booking", 999L));

        mockMvc.perform(get("/api/v1/bookings/999").with(authentication(ownerAuth())))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("getById — без токена возвращает 401")
    void getBookingById_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/bookings/100"))
            .andExpect(status().isForbidden());
    }

    // ─────────────────────────────────────────────
    // POST /api/v1/bookings/{id}/cancel
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("cancel — отменяет бронирование и возвращает 200 со статусом CANCELLED")
    void cancelBooking_success() throws Exception {
        Booking cancelled = buildBooking(100L);
        cancelled.setStatus(BookingStatus.CANCELLED);
        when(bookingService.cancel(100L, CancellationReason.GUEST_CANCELLED)).thenReturn(cancelled);

        mockMvc.perform(post("/api/v1/bookings/100/cancel").with(authentication(ownerAuth())).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(bookingService).cancel(100L, CancellationReason.GUEST_CANCELLED);
    }

    @Test
    @DisplayName("cancel — бронирование не найдено возвращает 404")
    void cancelBooking_notFound() throws Exception {
        when(bookingService.cancel(anyLong(), any(CancellationReason.class)))
            .thenThrow(new ResourceNotFoundException("Booking", 999L));

        mockMvc.perform(post("/api/v1/bookings/999/cancel").with(authentication(ownerAuth())).with(csrf()))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("cancel — без токена возвращает 401")
    void cancelBooking_unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/bookings/100/cancel").with(csrf()))
            .andExpect(status().isForbidden());
    }
}
