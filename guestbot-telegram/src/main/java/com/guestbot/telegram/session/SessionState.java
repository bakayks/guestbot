package com.guestbot.telegram.session;

public enum SessionState {
    IDLE,
    SELECTING_HOTEL,
    COLLECTING_GUEST_NAME,
    COLLECTING_GUEST_PHONE,
    COLLECTING_CHECK_IN,
    COLLECTING_CHECK_OUT,
    AWAITING_PAYMENT,
    ESCALATED_TO_OWNER
}
