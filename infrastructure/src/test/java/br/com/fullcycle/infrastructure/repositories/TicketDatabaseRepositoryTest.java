package br.com.fullcycle.infrastructure.repositories;

import br.com.fullcycle.IntegrationTest;
import br.com.fullcycle.domain.customer.CustomerId;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.ticket.Ticket;
import br.com.fullcycle.domain.event.ticket.TicketRepository;
import br.com.fullcycle.domain.event.ticket.TicketStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TicketDatabaseRepositoryTest extends IntegrationTest {

    @Autowired
    private TicketRepository ticketRepository;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve buscar os ingressos de um evento")
    public void testTicketsByEventId() throws Exception {
        // given
        final var anEventId = EventId.unique();
        final var anotherEventId = EventId.unique();

        final var aTicket = Ticket.newTicket(CustomerId.unique(), anEventId);
        final var aTicket2 = Ticket.newTicket(CustomerId.unique(), anEventId);
        final var aTicketFromAnotherEvent = Ticket.newTicket(CustomerId.unique(), anotherEventId);

        ticketRepository.create(aTicket);
        ticketRepository.create(aTicket2);
        ticketRepository.create(aTicketFromAnotherEvent);

        // when
        final var actualTickets = ticketRepository.ticketsByEventId(anEventId);

        // then
        Assertions.assertEquals(2, actualTickets.size());
        Assertions.assertTrue(actualTickets.stream()
                .allMatch(it -> it.eventId().equals(anEventId)));
    }

    @Test
    @DisplayName("Deve persistir o cancelamento de um ingresso")
    public void testCancelTicketPersistence() throws Exception {
        // given
        final var aTicket = Ticket.newTicket(CustomerId.unique(), EventId.unique());
        ticketRepository.create(aTicket);

        // when
        aTicket.cancel();
        ticketRepository.update(aTicket);

        // then
        final var actualTicket = ticketRepository.ticketOfId(aTicket.ticketId()).get();
        Assertions.assertEquals(TicketStatus.CANCELLED, actualTicket.status());
    }
}
