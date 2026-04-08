package com.guestbot.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guestbot.api.mapper.KnowledgeBaseMapperImpl;
import com.guestbot.api.security.SecurityConfig;
import com.guestbot.core.entity.KnowledgeBase;
import com.guestbot.core.exception.ResourceNotFoundException;
import com.guestbot.service.auth.JwtService;
import com.guestbot.service.knowledge.KnowledgeBaseService;
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

@WebMvcTest(KnowledgeBaseController.class)
@Import({SecurityConfig.class, KnowledgeBaseMapperImpl.class})
@DisplayName("KnowledgeBaseController")
class KnowledgeBaseControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean KnowledgeBaseService knowledgeBaseService;
    @MockBean JwtService jwtService;

    private static final Long HOTEL_ID = 1L;
    private static final Long OWNER_ID = 10L;
    private static final String BASE = "/api/v1/hotels/" + HOTEL_ID + "/knowledge-base";

    private UsernamePasswordAuthenticationToken ownerAuth() {
        return new UsernamePasswordAuthenticationToken(
            OWNER_ID, null, List.of(new SimpleGrantedAuthority("ROLE_OWNER"))
        );
    }

    private KnowledgeBase buildKb(Long id, String question, String answer) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(id);
        kb.setCategory("Общее");
        kb.setQuestion(question);
        kb.setAnswer(answer);
        return kb;
    }

    // ─────────────────────────────────────────────
    // GET /api/v1/hotels/{hotelId}/knowledge-base
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("getAll — возвращает список записей базы знаний")
    void getAll_success() throws Exception {
        KnowledgeBase kb1 = buildKb(1L, "Время заезда?", "С 14:00");
        KnowledgeBase kb2 = buildKb(2L, "Есть ли парковка?", "Да, бесплатная");
        when(knowledgeBaseService.getByHotel(HOTEL_ID)).thenReturn(List.of(kb1, kb2));

        mockMvc.perform(get(BASE).with(authentication(ownerAuth())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].question").value("Время заезда?"))
            .andExpect(jsonPath("$[0].answer").value("С 14:00"))
            .andExpect(jsonPath("$[1].question").value("Есть ли парковка?"));

        verify(knowledgeBaseService).getByHotel(HOTEL_ID);
    }

    @Test
    @DisplayName("getAll — пустой список если нет записей")
    void getAll_empty() throws Exception {
        when(knowledgeBaseService.getByHotel(HOTEL_ID)).thenReturn(List.of());

        mockMvc.perform(get(BASE).with(authentication(ownerAuth())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("getAll — без токена возвращает 401")
    void getAll_unauthorized() throws Exception {
        mockMvc.perform(get(BASE))
            .andExpect(status().isForbidden());
    }

    // ─────────────────────────────────────────────
    // POST /api/v1/hotels/{hotelId}/knowledge-base
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("create — создаёт запись базы знаний и возвращает 200")
    void create_success() throws Exception {
        KnowledgeBase saved = buildKb(5L, "Есть ли Wi-Fi?", "Да, бесплатный");
        when(knowledgeBaseService.create(eq(HOTEL_ID), any(KnowledgeBase.class))).thenReturn(saved);

        mockMvc.perform(post(BASE).with(authentication(ownerAuth())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "category", "Удобства",
                    "question", "Есть ли Wi-Fi?",
                    "answer", "Да, бесплатный"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(5))
            .andExpect(jsonPath("$.question").value("Есть ли Wi-Fi?"))
            .andExpect(jsonPath("$.answer").value("Да, бесплатный"));

        verify(knowledgeBaseService).create(eq(HOTEL_ID), any(KnowledgeBase.class));
    }

    @Test
    @DisplayName("create — без токена возвращает 401")
    void create_unauthorized() throws Exception {
        mockMvc.perform(post(BASE).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "category", "Общее",
                    "question", "Вопрос?",
                    "answer", "Ответ"
                ))))
            .andExpect(status().isForbidden());
    }

    // ─────────────────────────────────────────────
    // PUT /api/v1/hotels/{hotelId}/knowledge-base/{id}
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("update — обновляет запись базы знаний и возвращает 200")
    void update_success() throws Exception {
        KnowledgeBase updated = buildKb(5L, "Есть ли Wi-Fi?", "Да, в номерах и лобби");
        when(knowledgeBaseService.update(eq(5L), any(KnowledgeBase.class))).thenReturn(updated);

        mockMvc.perform(put(BASE + "/5").with(authentication(ownerAuth())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "category", "Удобства",
                    "question", "Есть ли Wi-Fi?",
                    "answer", "Да, в номерах и лобби"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.answer").value("Да, в номерах и лобби"));

        verify(knowledgeBaseService).update(eq(5L), any(KnowledgeBase.class));
    }

    @Test
    @DisplayName("update — запись не найдена возвращает 404")
    void update_notFound() throws Exception {
        when(knowledgeBaseService.update(anyLong(), any(KnowledgeBase.class)))
            .thenThrow(new ResourceNotFoundException("KnowledgeBase", 999L));

        mockMvc.perform(put(BASE + "/999").with(authentication(ownerAuth())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "category", "X",
                    "question", "Q?",
                    "answer", "A"
                ))))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("update — без токена возвращает 401")
    void update_unauthorized() throws Exception {
        mockMvc.perform(put(BASE + "/5").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "category", "X", "question", "Q?", "answer", "A"
                ))))
            .andExpect(status().isForbidden());
    }

    // ─────────────────────────────────────────────
    // DELETE /api/v1/hotels/{hotelId}/knowledge-base/{id}
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("delete — удаляет запись и возвращает 204")
    void delete_success() throws Exception {
        doNothing().when(knowledgeBaseService).delete(5L);

        mockMvc.perform(delete(BASE + "/5").with(authentication(ownerAuth())).with(csrf()))
            .andExpect(status().isNoContent());

        verify(knowledgeBaseService).delete(5L);
    }

    @Test
    @DisplayName("delete — запись не найдена возвращает 404")
    void delete_notFound() throws Exception {
        doThrow(new ResourceNotFoundException("KnowledgeBase", 999L))
            .when(knowledgeBaseService).delete(999L);

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
