package com.guestbot.core.exception;

public class GuestBotException extends RuntimeException {
    public GuestBotException(String message) {
        super(message);
    }
    public GuestBotException(String message, Throwable cause) {
        super(message, cause);
    }
}
