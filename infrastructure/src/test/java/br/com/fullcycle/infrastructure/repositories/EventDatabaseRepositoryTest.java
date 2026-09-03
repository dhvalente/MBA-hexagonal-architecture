package br.com.fullcycle.infrastructure.repositories;

import br.com.fullcycle.IntegrationTest;
import br.com.fullcycle.domain.event.Event;
import br.com.fullcycle.domain.event.EventRepository;
import br.com.fullcycle.domain.event.EventStatus;
import br.com.fullcycle.domain.partner.Partner;
import br.com.fullcycle.domain.partner.PartnerRepository;
import br.com.fullcycle.infrastructure.jpa.repositories.OutboxJpaRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EventDatabaseRepositoryTest extends IntegrationTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private OutboxJpaRepository outboxJpaRepository;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        partnerRepository.deleteAll();
        outboxJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve persistir o cancelamento de um evento e gravar o evento de domínio na outbox")
    public void testCancelEventPersistence() throws Exception {
        // given
        final var aPartner = partnerRepository.create(Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com"));
        final var anEvent = eventRepository.create(Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner));

        // when
        anEvent.cancel();
        eventRepository.update(anEvent);

        // then
        final var actualEvent = eventRepository.eventOfId(anEvent.eventId()).get();
        Assertions.assertEquals(EventStatus.CANCELLED, actualEvent.status());

        final var actualOutboxContents = outboxJpaRepository.findAll();
        Assertions.assertTrue(
                actualOutboxContents.iterator().next().content().contains("event.cancelled")
        );
    }
}
