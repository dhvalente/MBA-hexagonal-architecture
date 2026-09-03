package br.com.fullcycle.domain.event;

import br.com.fullcycle.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record EventCancelled(
        String domainEventId,
        String type,
        String eventId,
        Instant occurredOn
) implements DomainEvent {

    public static EventCancelled of(final EventId eventId) {
        return new EventCancelled(UUID.randomUUID().toString(), "event.cancelled", eventId.value(), Instant.now());
    }
}
