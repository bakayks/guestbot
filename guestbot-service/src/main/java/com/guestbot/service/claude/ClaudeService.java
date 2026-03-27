package com.guestbot.service.claude;

import com.fasterxml.jackson.databind.JsonNode;
import com.guestbot.core.entity.Hotel;
import com.guestbot.core.entity.Room;
import com.guestbot.repository.RoomRepository;
import com.guestbot.service.knowledge.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ClaudeService {

    private final WebClient claudeWebClient;
    private final KnowledgeBaseService knowledgeBaseService;
    private final RoomRepository roomRepository;

    @Value("${claude.haiku-model:claude-haiku-4-5-20251001}")
    private String model;

    @Value("${claude.max-tokens:1024}")
    private int maxTokens;

    public ClaudeService(@Qualifier("claudeWebClient") WebClient claudeWebClient,
                         KnowledgeBaseService knowledgeBaseService,
                         RoomRepository roomRepository) {
        this.claudeWebClient = claudeWebClient;
        this.knowledgeBaseService = knowledgeBaseService;
        this.roomRepository = roomRepository;
    }

    /**
     * Отвечает на вопрос гостя в контексте конкретного отеля.
     *
     * @param hotel   отель (не null)
     * @param history история в формате [{role: user/assistant, content: ...}]
     * @param userText последнее сообщение пользователя
     * @return текст ответа от Claude
     */
    public String chat(Hotel hotel, List<Map<String, String>> history, String userText) {
        String systemPrompt = buildHotelSystemPrompt(hotel);
        List<Map<String, Object>> messages = buildMessages(history, userText);

        return callClaude(systemPrompt, messages);
    }

    /**
     * Помогает гостю выбрать отель из списка доступных.
     *
     * @param availableHotels список активных отелей
     * @param history         история диалога
     * @param userText        последнее сообщение
     * @return текст ответа от Claude
     */
    public String discover(List<Hotel> availableHotels, List<Map<String, String>> history, String userText) {
        String systemPrompt = buildDiscoverySystemPrompt(availableHotels);
        List<Map<String, Object>> messages = buildMessages(history, userText);

        return callClaude(systemPrompt, messages);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private String buildHotelSystemPrompt(Hotel hotel) {
        StringBuilder sb = new StringBuilder();
        sb.append("Ты — AI-консьерж гостиницы \"").append(hotel.getName()).append("\".\n");
        sb.append("Ты общаешься с гостем в Telegram. Отвечай вежливо, кратко и по делу.\n");
        sb.append("Отвечай на том же языке, на котором пишет гость.\n\n");

        sb.append("## Информация об отеле:\n");
        sb.append("Название: ").append(hotel.getName()).append("\n");
        if (hotel.getCity() != null) sb.append("Город: ").append(hotel.getCity()).append("\n");
        if (hotel.getAddress() != null) sb.append("Адрес: ").append(hotel.getAddress()).append("\n");
        if (hotel.getPhone() != null) sb.append("Телефон: ").append(hotel.getPhone()).append("\n");
        if (hotel.getDescription() != null) sb.append("Описание: ").append(hotel.getDescription()).append("\n");
        if (hotel.getCheckInTime() != null) sb.append("Заезд с: ").append(hotel.getCheckInTime()).append("\n");
        if (hotel.getCheckOutTime() != null) sb.append("Выезд до: ").append(hotel.getCheckOutTime()).append("\n");
        if (hotel.getPetsAllowed() != null)
            sb.append("Питомцы: ").append(hotel.getPetsAllowed() ? "разрешены" : "не разрешены").append("\n");
        if (hotel.getCancellationPolicy() != null)
            sb.append("Политика отмены: ").append(hotel.getCancellationPolicy()).append("\n");

        // Комнаты с ценами и наличием фото
        List<Room> rooms = roomRepository.findByHotelIdAndActiveTrueWithPhotos(hotel.getId());
        if (!rooms.isEmpty()) {
            sb.append("\n## Номера:\n");
            for (Room room : rooms) {
                sb.append("- ").append(room.getType());
                sb.append(" | ").append(room.getPricePerNight()).append(" сом/ночь");
                sb.append(" | вместимость: ").append(room.getCapacity()).append(" чел.");
                if (!room.getPhotos().isEmpty())
                    sb.append(" | 📷 есть фото");
                if (room.getDescription() != null)
                    sb.append(" | ").append(room.getDescription());
                sb.append("\n");
            }
        }

        String kb = knowledgeBaseService.formatForPrompt(hotel.getId());
        if (!kb.isEmpty()) {
            sb.append("\n").append(kb);
        }

        sb.append("\n## Инструкции:\n");
        sb.append("- Если гость хочет забронировать номер, предложи нажать кнопку \"Забронировать\".\n");
        sb.append("- Фотографии номеров есть в системе. НИКОГДА не говори что не можешь отправить фото.\n");
        sb.append("  Когда гость просит фото — скажи что фотографии каждого номера будут показаны\n");
        sb.append("  автоматически в процессе бронирования при выборе номера.\n");
        sb.append("- Если не знаешь ответа — предложи связаться с администратором по телефону.\n");
        sb.append("- Не придумывай информацию, которой нет выше.\n");

        return sb.toString();
    }

    private String buildDiscoverySystemPrompt(List<Hotel> hotels) {
        StringBuilder sb = new StringBuilder();
        sb.append("Ты — AI-ассистент платформы бронирования гостиниц Guestbot.\n");
        sb.append("Помоги гостю выбрать подходящую гостиницу из списка ниже.\n");
        sb.append("Отвечай на том же языке, на котором пишет гость.\n\n");

        sb.append("## Доступные гостиницы:\n");
        for (Hotel h : hotels) {
            sb.append("- ").append(h.getName());
            if (h.getCity() != null) sb.append(" (").append(h.getCity()).append(")");
            sb.append("\n");
            if (h.getDescription() != null) sb.append("  ").append(h.getDescription()).append("\n");
        }

        sb.append("\n## Инструкции:\n");
        sb.append("- Спроси у гостя предпочтения: город, даты, бюджет, количество гостей.\n");
        sb.append("- Порекомендуй подходящие гостиницы из списка.\n");
        sb.append("- НИКОГДА не проси вводить номер из списка. Гость выбирает гостиницу нажатием на кнопку под сообщением.\n");
        sb.append("- Когда гость определился — скажи что кнопки с гостиницами уже показаны ниже.\n");

        return sb.toString();
    }

    private List<Map<String, Object>> buildMessages(List<Map<String, String>> history, String userText) {
        List<Map<String, Object>> messages = new ArrayList<>();

        for (Map<String, String> entry : history) {
            String role = entry.get("role");
            String content = entry.get("content");
            if ("user".equals(role) || "assistant".equals(role)) {
                messages.add(Map.of("role", role, "content", content));
            }
        }

        messages.add(Map.of("role", "user", "content", userText));
        return messages;
    }

    private String callClaude(String systemPrompt, List<Map<String, Object>> messages) {
        Map<String, Object> requestBody = Map.of(
            "model", model,
            "max_tokens", maxTokens,
            "system", systemPrompt,
            "messages", messages
        );

        try {
            JsonNode response = claudeWebClient.post()
                .uri("/v1/messages")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

            if (response != null && response.has("content")) {
                JsonNode content = response.get("content");
                if (content.isArray() && !content.isEmpty()) {
                    return content.get(0).get("text").asText();
                }
            }

            log.warn("Unexpected Claude response structure: {}", response);
            return fallbackMessage();

        } catch (WebClientResponseException e) {
            log.error("Claude API {} error. Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return fallbackMessage();
        } catch (Exception e) {
            log.error("Claude API call failed", e);
            return fallbackMessage();
        }
    }

    private String fallbackMessage() {
        return "Извините, сейчас не могу обработать ваш запрос. Пожалуйста, попробуйте позже или свяжитесь с администратором напрямую.";
    }
}
