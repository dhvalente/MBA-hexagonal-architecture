package br.com.fullcycle.infrastructure;

import br.com.fullcycle.IntegrationTest;
import br.com.fullcycle.domain.DomainEvent;
import br.com.fullcycle.domain.customer.CustomerId;
import br.com.fullcycle.domain.event.EventCancelled;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.ticket.Ticket;
import br.com.fullcycle.domain.event.ticket.TicketRepository;
import br.com.fullcycle.domain.event.ticket.TicketStatus;
import br.com.fullcycle.infrastructure.jpa.entities.OutboxEntity;
import br.com.fullcycle.infrastructure.jpa.repositories.OutboxJpaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

class EventCancellationE2ETest extends IntegrationTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private OutboxJpaRepository outboxJpaRepository;

    @Autowired
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        outboxJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve cancelar os ingressos de um evento de forma assíncrona ao publicar EventCancelled na outbox")
    public void testEventCancellationCascadesToTicketsAsynchronously() throws Exception {
        // given
        final var anEventId = EventId.unique();

        final var aTicket = Ticket.newTicket(CustomerId.unique(), anEventId);
        final var aTicket2 = Ticket.newTicket(CustomerId.unique(), anEventId);

        ticketRepository.create(aTicket);
        ticketRepository.create(aTicket2);

        // when: publica o EventCancelled pelo caminho real (outbox -> OutboxRelay -> ConsumerQueueGateway)
        outboxJpaRepository.save(OutboxEntity.of(EventCancelled.of(anEventId), this::toJson));

        // then: aguarda o relay (fixedRate=2000) + consumer assíncrono cascatearem o cancelamento
        awaitUntil(
                () -> ticketRepository.ticketsByEventId(anEventId).stream()
                        .allMatch(it -> it.status() == TicketStatus.CANCELLED),
                Duration.ofSeconds(10)
        );
    }

    private String toJson(final DomainEvent domainEvent) {
        try {
            return this.mapper.writeValueAsString(domainEvent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private void awaitUntil(final Supplier<Boolean> condition, final Duration timeout) throws InterruptedException {
        final var deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            if (condition.get()) {
                return;
            }
            Thread.sleep(250);
        }
        Assertions.fail("Condition not met within " + timeout);
    }
}
