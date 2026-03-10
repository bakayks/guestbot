package com.guestbot.core.exception;

public class RoomNotAvailableException extends GuestBotException {
    public RoomNotAvailableException(Long roomId) {
        super("Room " + roomId + " is not available for requested dates");
    }
}
