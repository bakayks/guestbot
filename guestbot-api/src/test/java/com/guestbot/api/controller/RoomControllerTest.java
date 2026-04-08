package com.guestbot.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guestbot.api.mapper.RoomMapperImpl;
import com.guestbot.api.security.SecurityConfig;
import com.guestbot.core.entity.Room;
import com.guestbot.core.exception.ResourceNotFoundException;
import com.guestbot.service.auth.JwtService;
import com.guestbot.service.room.RoomService;
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
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoomController.class)
@Import({SecurityConfig.class, RoomMapperImpl.class})
@DisplayName("RoomController")
class RoomControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean RoomService roomService;
    @MockBean JwtService jwtService;

    private static final Long HOTEL_ID = 1L;
    private static final Long OWNER_ID = 10L;
    private static final String BASE = "/api/v1/hotels/" + HOTEL_ID + "/rooms";

    private UsernamePasswordAuthenticationToken ownerAuth() {
        return new UsernamePasswordAuthenticationToken(
            OWNER_ID, null, List.of(new SimpleGrantedAuthority("ROLE_OWNER"))
        );
    }

    private Room buildRoom(Long id, String type) {
        Room room = new Room();
        room.setId(id);
        room.setType(type);
        room.setCapacity(2);
        room.setCount(3);
        room.setPricePerNight(BigDecimal.valueOf(3500));
        room.setActive(true);
        return room;
    }

    // ─────────────────────────────────────────────
    // GET /api/v1/hotels/{hotelId}/rooms
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("getByHotel — возвращает список номеров")
    void getByHotel_success() throws Exception {
        Room r1 = buildRoom(1L, "Стандарт");
        Room r2 = buildRoom(2L, "Люкс");
        when(roomService.getByHotel(HOTEL_ID)).thenReturn(List.of(r1, r2));

        mockMvc.perform(get(BASE).with(authentication(ownerAuth())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].type").value("Стандарт"))
            .andExpect(jsonPath("$[1].type").value("Люкс"));

        verify(roomService).getByHotel(HOTEL_ID);
    }

    @Test
    @DisplayName("getByHotel — без токена возвращает 401")
    void getByHotel_unauthorized() throws Exception {
        mockMvc.perform(get(BASE))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("getByHotel — пустой список если нет номеров")
    void getByHotel_empty() throws Exception {
        when(roomService.getByHotel(HOTEL_ID)).thenReturn(List.of());

        mockMvc.perform(get(BASE).with(authentication(ownerAuth())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    // ─────────────────────────────────────────────
    // GET /api/v1/hotels/{hotelId}/rooms/{id}
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("getById — возвращает номер по id")
    void getById_success() throws Exception {
        Room room = buildRoom(5L, "Стандарт");
        when(roomService.getById(5L)).thenReturn(room);

        mockMvc.perform(get(BASE + "/5").with(authentication(ownerAuth())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(5))
            .andExpect(jsonPath("$.type").value("Стандарт"))
            .andExpect(jsonPath("$.capacity").value(2))
            .andExpect(jsonPath("$.pricePerNight").value(3500));
    }

    @Test
    @DisplayName("getById — несуществующий id возвращает 404")
    void getById_notFound() throws Exception {
        when(roomService.getById(999L))
            .thenThrow(new ResourceNotFoundException("Room", 999L));

        mockMvc.perform(get(BASE + "/999").with(authentication(ownerAuth())))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("getById — без токена возвращает 401")
    void getById_unauthorized() throws Exception {
        mockMvc.perform(get(BASE + "/5"))
            .andExpect(status().isForbidden());
    }

    // ─────────────────────────────────────────────
    // POST /api/v1/hotels/{hotelId}/rooms
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("create — создаёт номер и возвращает 200")
    void create_success() throws Exception {
        Room saved = buildRoom(20L, "Стандарт");
        when(roomService.create(eq(HOTEL_ID), any(Room.class))).thenReturn(saved);

        mockMvc.perform(post(BASE).with(authentication(ownerAuth())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "type", "Стандарт",
                    "capacity", 2,
                    "count", 3,
                    "pricePerNight", 3500
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(20))
            .andExpect(jsonPath("$.type").value("Стандарт"));

        verify(roomService).create(eq(HOTEL_ID), any(Room.class));
    }

    @Test
    @DisplayName("create — без токена возвращает 401")
    void create_unauthorized() throws Exception {
        mockMvc.perform(post(BASE).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("type", "Стандарт"))))
            .andExpect(status().isForbidden());
    }

    // ─────────────────────────────────────────────
    // PUT /api/v1/hotels/{hotelId}/rooms/{id}
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("update — обновляет номер и возвращает 200")
    void update_success() throws Exception {
        Room updated = buildRoom(5L, "Люкс");
        updated.setPricePerNight(BigDecimal.valueOf(7000));
        when(roomService.update(eq(5L), any(Room.class))).thenReturn(updated);

        mockMvc.perform(put(BASE + "/5").with(authentication(ownerAuth())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "type", "Люкс",
                    "capacity", 2,
                    "count", 1,
                    "pricePerNight", 7000
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type").value("Люкс"))
            .andExpect(jsonPath("$.pricePerNight").value(7000));

        verify(roomService).update(eq(5L), any(Room.class));
    }

    @Test
    @DisplayName("update — номер не найден возвращает 404")
    void update_notFound() throws Exception {
        when(roomService.update(anyLong(), any(Room.class)))
            .thenThrow(new ResourceNotFoundException("Room", 999L));

        mockMvc.perform(put(BASE + "/999").with(authentication(ownerAuth())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "type", "Стандарт", "capacity", 2, "count", 1, "pricePerNight", 3000
                ))))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("update — без токена возвращает 401")
    void update_unauthorized() throws Exception {
        mockMvc.perform(put(BASE + "/5").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("type", "X"))))
            .andExpect(status().isForbidden());
    }

    // ─────────────────────────────────────────────
    // DELETE /api/v1/hotels/{hotelId}/rooms/{id}
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("delete — удаляет номер и возвращает 204")
    void delete_success() throws Exception {
        doNothing().when(roomService).delete(5L);

        mockMvc.perform(delete(BASE + "/5").with(authentication(ownerAuth())).with(csrf()))
            .andExpect(status().isNoContent());

        verify(roomService).delete(5L);
    }

    @Test
    @DisplayName("delete — номер не найден возвращает 404")
    void delete_notFound() throws Exception {
        doThrow(new ResourceNotFoundException("Room", 999L))
            .when(roomService).delete(999L);

        mockMvc.perform(delete(BASE + "/999").with(authentication(ownerAuth())).with(csrf()))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("delete — без токена возвращает 401")
    void delete_unauthorized() throws Exception {
        mockMvc.perform(delete(BASE + "/5").with(csrf()))
            .andExpect(status().isForbidden());
    }
}
