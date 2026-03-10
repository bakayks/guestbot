package com.guestbot.telegram.notification;

import com.guestbot.core.entity.Hotel;
import com.guestbot.core.event.DomainEvents.EscalationRequestedEvent;
import com.guestbot.core.event.DomainEvents.OwnerRepliedEvent;
import com.guestbot.service.hotel.HotelService;
import com.guestbot.telegram.handler.TelegramClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramEventListener {

    private final TelegramClient telegramClient;
    private final HotelService hotelService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("telegramExecutor")
    public void onEscalationRequested(EscalationRequestedEvent event) {
        try {
            Hotel hotel = hotelService.getById(event.hotelId());
            String ownerChatId = hotel.getOwner().getTelegramChatId();

            if (ownerChatId == null) {
                log.warn("Owner has no telegramChatId for hotel {}", event.hotelId());
                return;
            }

            String text = String.format("""
                ⚠️ *Вопрос требует вашего ответа*
                
                👤 Гость: %s
                💬 Вопрос: %s
                """,
                event.guestPhone(),
                event.guestQuestion()
            );

            telegramClient.sendMessage(hotel.getTelegramBotToken(), Long.parseLong(ownerChatId), text);
        } catch (Exception e) {
            log.error("Failed to send escalation notification for conversation {}",
                event.conversationId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("telegramExecutor")
    public void onOwnerReplied(OwnerRepliedEvent event) {
        // Когда владелец отвечает через админку — отправляем гостю в Telegram
        // botToken берем через conversationId → hotel → botToken
        log.info("Owner replied to conversation {}", event.conversationId());
        // TODO: достать botToken через conversationId
        telegramClient.sendMessage(null, event.telegramChatId(), event.replyText());
    }
}
