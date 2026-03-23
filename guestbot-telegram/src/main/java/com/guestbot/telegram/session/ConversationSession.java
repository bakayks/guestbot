package com.guestbot.telegram.session;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSession {
    private Long hotelId;
    private Long roomId;
    private Long chatId;
    private SessionState state = SessionState.IDLE;
    private String guestName;
    private String guestPhone;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private String roomName;
    private Long pendingBookingId;
    private List<Map<String, String>> history = new ArrayList<>();

    public ConversationSession(Long hotelId, Long chatId) {
        this.hotelId = hotelId;
        this.chatId = chatId;
    }
}
