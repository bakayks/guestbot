package com.guestbot.core.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DomainEventPublisher {

    private final ApplicationEventPublisher publisher;

    public void publish(Object event) {
        log.debug("Publishing domain event: {}", event.getClass().getSimpleName());
        publisher.publishEvent(event);
    }
}
