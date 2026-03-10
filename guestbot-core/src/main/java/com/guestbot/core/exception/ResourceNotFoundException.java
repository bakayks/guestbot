package com.guestbot.core.exception;

public class ResourceNotFoundException extends GuestBotException {
    public ResourceNotFoundException(String resource, Long id) {
        super(resource + " not found with id: " + id);
    }
}
