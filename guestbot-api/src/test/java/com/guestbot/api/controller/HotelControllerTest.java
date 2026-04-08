package com.guestbot.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guestbot.api.mapper.HotelMapperImpl;
import com.guestbot.api.security.SecurityConfig;
import com.guestbot.core.entity.Hotel;
import com.guestbot.core.exception.ResourceNotFoundException;
import com.guestbot.service.auth.JwtService;
import com.guestbot.service.hotel.HotelService;
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

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HotelController.class)
@Import({SecurityConfig.class, HotelMapperImpl.class})
@DisplayName("HotelController")
class HotelControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean HotelService hotelService;
    @MockBean JwtService jwtService;

    private static final String BASE = "/api/v1/hotels";
    private static final Long OWNER_ID = 1L;

    private UsernamePasswordAuthenticationToken ownerAuth() {
        return new UsernamePasswordAuthenticationToken(
            OWNER_ID, null, List.of(new SimpleGrantedAuthority("ROLE_OWNER"))
        );
    }

    private Hotel buildHotel(Long id, String name) {
        Hotel hotel = new Hotel();
        hotel.setId(id);
        hotel.setName(name);
        hotel.setActive(true);
        return hotel;
    }

    // ─────────────────────────────────────────────
    // GET /api/v1/hotels
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("getMyHotels — возвращает список отелей владельца")
    void getMyHotels_success() throws Exception {
        Hotel h1 = buildHotel(1L, "Отель Москва");
        Hotel h2 = buildHotel(2L, "Отель Сочи");
        when(hotelService.getByOwner(OWNER_ID)).thenReturn(List.of(h1, h2));

        mockMvc.perform(get(BASE).with(authentication(ownerAuth())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].name").value("Отель Москва"))
            .andExpect(jsonPath("$[1].name").value("Отель Сочи"));

        verify(hotelService).getByOwner(OWNER_ID);
    }

    @Test
    @DisplayName("getMyHotels — без токена возвращает 401")
    void getMyHotels_unauthorized() throws Exception {
        mockMvc.perform(get(BASE))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("getMyHotels — пустой список если у владельца нет отелей")
    void getMyHotels_empty() throws Exception {
        when(hotelService.getByOwner(OWNER_ID)).thenReturn(List.of());

        mockMvc.perform(get(BASE).with(authentication(ownerAuth())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    // ─────────────────────────────────────────────
    // GET /api/v1/hotels/{id}
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("getById — возвращает отель по id")
    void getById_success() throws Exception {
        Hotel hotel = buildHotel(5L, "Гранд Отель");
        when(hotelService.getById(5L)).thenReturn(hotel);

        mockMvc.perform(get(BASE + "/5").with(authentication(ownerAuth())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(5))
            .andExpect(jsonPath("$.name").value("Гранд Отель"));
    }

    @Test
    @DisplayName("getById — несуществующий id возвращает 404")
    void getById_notFound() throws Exception {
        when(hotelService.getById(999L))
            .thenThrow(new ResourceNotFoundException("Hotel", 999L));

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
    // POST /api/v1/hotels
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("create — создаёт отель и возвращает 200")
    void create_success() throws Exception {
        Hotel saved = buildHotel(10L, "Новый Отель");
        when(hotelService.create(eq(OWNER_ID), any(Hotel.class))).thenReturn(saved);

        mockMvc.perform(post(BASE).with(authentication(ownerAuth())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", "Новый Отель"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.name").value("Новый Отель"));

        verify(hotelService).create(eq(OWNER_ID), any(Hotel.class));
    }

    @Test
    @DisplayName("create — без токена возвращает 401")
    void create_unauthorized() throws Exception {
        mockMvc.perform(post(BASE).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", "Новый Отель"))))
            .andExpect(status().isForbidden());
    }

    // ─────────────────────────────────────────────
    // PUT /api/v1/hotels/{id}
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("update — обновляет отель и возвращает 200")
    void update_success() throws Exception {
        Hotel updated = buildHotel(5L, "Обновлённый Отель");
        when(hotelService.update(eq(5L), eq(OWNER_ID), any(Hotel.class))).thenReturn(updated);

        mockMvc.perform(put(BASE + "/5").with(authentication(ownerAuth())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", "Обновлённый Отель"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Обновлённый Отель"));

        verify(hotelService).update(eq(5L), eq(OWNER_ID), any(Hotel.class));
    }

    @Test
    @DisplayName("update — отель не найден возвращает 404")
    void update_notFound() throws Exception {
        when(hotelService.update(anyLong(), anyLong(), any(Hotel.class)))
            .thenThrow(new ResourceNotFoundException("Hotel", 999L));

        mockMvc.perform(put(BASE + "/999").with(authentication(ownerAuth())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", "X"))))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("update — без токена возвращает 401")
    void update_unauthorized() throws Exception {
        mockMvc.perform(put(BASE + "/5").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", "X"))))
            .andExpect(status().isForbidden());
    }

    // ─────────────────────────────────────────────
    // DELETE /api/v1/hotels/{id}
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("delete — удаляет отель и возвращает 204")
    void delete_success() throws Exception {
        doNothing().when(hotelService).delete(5L, OWNER_ID);

        mockMvc.perform(delete(BASE + "/5").with(authentication(ownerAuth())).with(csrf()))
            .andExpect(status().isNoContent());

        verify(hotelService).delete(5L, OWNER_ID);
    }

    @Test
    @DisplayName("delete — отель не найден возвращает 404")
    void delete_notFound() throws Exception {
        doThrow(new ResourceNotFoundException("Hotel", 999L))
            .when(hotelService).delete(999L, OWNER_ID);

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
