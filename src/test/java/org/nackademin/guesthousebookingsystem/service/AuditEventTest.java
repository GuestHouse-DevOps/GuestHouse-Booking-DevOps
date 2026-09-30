package org.nackademin.guesthousebookingsystem.service;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.nackademin.guesthousebookingsystem.dto.BookingDto;
import org.nackademin.guesthousebookingsystem.dto.RoomDto;
import org.nackademin.guesthousebookingsystem.entity.AuditEvent;
import org.nackademin.guesthousebookingsystem.entity.RoomType;
import org.nackademin.guesthousebookingsystem.repository.AuditEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles({"test", "local"})
@Transactional
class AuditEventTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @RegisterExtension
    static WireMockExtension customerService = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void customerServiceUrl(DynamicPropertyRegistry registry) {
        registry.add("customer.service.url", customerService::baseUrl);
    }

    private RoomDto room;

    @BeforeEach
    void setUp() {
        customerService.stubFor(get(urlPathMatching("/api/customers/\\d+"))
                .willReturn(okJson("""
                        {"id": 1, "name": "Anna Andersson", "email": "anna@example.com", "phoneNumber": "0701234567"}
                        """)));
        room = roomService.saveRoom(new RoomDto(null, 201, RoomType.DOUBLE, 0, false));
    }

    @Test
    void bookingLifecycle_shouldRecordCreatedAndUpdated() {
        BookingDto booking = bookingService.saveBooking(new BookingDto(null, 1L, null, room,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3), null));
        bookingService.updateBooking(booking.getId(), new BookingDto(null, 1L, null, room,
                LocalDate.of(2026, 8, 2), LocalDate.of(2026, 8, 4), null));
        bookingService.cancelBooking(booking.getId());

        List<AuditEvent.Action> actions = auditEventRepository
                .findByEntityTypeAndEntityIdOrderByOccurredAtDesc(AuditEvent.Type.BOOKING, booking.getId())
                .stream().map(AuditEvent::getAction).toList();

        assertEquals(3, actions.size());
        assertTrue(actions.containsAll(List.of(
                AuditEvent.Action.CREATED, AuditEvent.Action.UPDATED)));
    }

    @Test
    void saveRoom_shouldRecordCreated() {
        List<AuditEvent> events = auditEventRepository
                .findByEntityTypeAndEntityIdOrderByOccurredAtDesc(AuditEvent.Type.ROOM, room.getId());

        assertEquals(1, events.size());
        assertEquals(AuditEvent.Action.CREATED, events.get(0).getAction());
        assertNotNull(events.get(0).getOccurredAt());
    }

    @Test
    void invalidBooking_shouldRecordNothing() {
        long before = auditEventRepository.count();

        assertThrows(IllegalArgumentException.class, () -> bookingService.saveBooking(
                new BookingDto(null, 1L, null, room,
                        LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 1), null)));

        assertEquals(before, auditEventRepository.count());
    }
}
