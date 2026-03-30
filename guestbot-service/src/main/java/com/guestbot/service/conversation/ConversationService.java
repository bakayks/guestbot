package com.guestbot.service.conversation;

import com.guestbot.core.entity.Conversation;
import com.guestbot.core.entity.Hotel;
import com.guestbot.core.entity.Message;
import com.guestbot.core.entity.Message.SenderType;
import com.guestbot.repository.ConversationRepository;
import com.guestbot.repository.HotelRepository;
import com.guestbot.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final HotelRepository hotelRepository;

    /**
     * Сохраняет сообщение гостя в историю переписки.
     * Если беседа не существует — создаёт её.
     * Выполняется асинхронно чтобы не блокировать ответ бота.
     */
    @Async("telegramExecutor")
    @Transactional
    public void saveGuestMessage(Long hotelId, Long telegramChatId, String text) {
        saveMessage(hotelId, telegramChatId, SenderType.GUEST, text, null, null);
    }

    @Async("telegramExecutor")
    @Transactional
    public void saveBotMessage(Long hotelId, Long telegramChatId, String text) {
        saveMessage(hotelId, telegramChatId, SenderType.BOT, text, null, null);
    }

    @Async("telegramExecutor")
    @Transactional
    public void saveGuestMessageWithContact(Long hotelId, Long telegramChatId,
                                            String text, String guestName, String guestPhone) {
        saveMessage(hotelId, telegramChatId, SenderType.GUEST, text, guestName, guestPhone);
    }

    /** Ищет последний диалог гостя за последние 24 часа. */
    @Transactional(readOnly = true)
    public Optional<Conversation> findRecentConversation(Long telegramChatId) {
        return conversationRepository.findRecentByTelegramChatId(
            telegramChatId,
            LocalDateTime.now().minusHours(24)
        );
    }

    private void saveMessage(Long hotelId, Long telegramChatId,
                              SenderType senderType, String text,
                              String guestName, String guestPhone) {
        try {
            Conversation conversation = conversationRepository
                .findByHotelIdAndTelegramChatId(hotelId, telegramChatId)
                .orElseGet(() -> createConversation(hotelId, telegramChatId));

            if (guestName != null) conversation.setGuestName(guestName);
            if (guestPhone != null) conversation.setGuestPhone(guestPhone);
            conversation.setLastMessageAt(LocalDateTime.now());
            conversationRepository.save(conversation);

            Message message = new Message();
            message.setConversation(conversation);
            message.setSenderType(senderType);
            message.setContent(text);
            messageRepository.save(message);

        } catch (Exception e) {
            log.error("Failed to save message for hotelId={} chatId={}", hotelId, telegramChatId, e);
        }
    }

    private Conversation createConversation(Long hotelId, Long telegramChatId) {
        Hotel hotel = hotelRepository.findById(hotelId)
            .orElseThrow(() -> new IllegalArgumentException("Hotel not found: " + hotelId));

        Conversation conversation = new Conversation();
        conversation.setHotel(hotel);
        conversation.setTelegramChatId(telegramChatId);
        conversation.setLastMessageAt(LocalDateTime.now());
        return conversationRepository.save(conversation);
    }
}