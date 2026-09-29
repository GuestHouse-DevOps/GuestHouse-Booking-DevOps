package org.nackademin.guesthousebookingsystem.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.nackademin.guesthousebookingsystem.client.CustomerClient;
import org.nackademin.guesthousebookingsystem.dto.BookingDto;
import org.nackademin.guesthousebookingsystem.dto.RoomDto;
import org.nackademin.guesthousebookingsystem.entity.AuditEvent;
import org.nackademin.guesthousebookingsystem.entity.RoomType;
import org.nackademin.guesthousebookingsystem.repository.AuditEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

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

    @MockitoBean
    private CustomerClient customerClient;

    private RoomDto room;

    @BeforeEach
    void setUp() {
        Mockito.when(customerClient.customerExists(Mockito.anyLong())).thenReturn(true);
        room = roomService.saveRoom(new RoomDto(null, 201, RoomType.DOUBLE, 0));
    }

    @Test
    void bookingLifecycle_shouldRecordCreatedUpdatedDeleted() {
        BookingDto booking = bookingService.saveBooking(new BookingDto(null, 1L, null, room,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3)));
        bookingService.updateBooking(booking.getId(), new BookingDto(null, 1L, null, room,
                LocalDate.of(2026, 8, 2), LocalDate.of(2026, 8, 4)));
        bookingService.deleteBooking(booking.getId());

        List<AuditEvent.Action> actions = auditEventRepository
                .findByEntityTypeAndEntityIdOrderByOccurredAtDesc(AuditEvent.Type.BOOKING, booking.getId())
                .stream().map(AuditEvent::getAction).toList();

        assertEquals(3, actions.size());
        assertTrue(actions.containsAll(List.of(
                AuditEvent.Action.CREATED, AuditEvent.Action.UPDATED, AuditEvent.Action.DELETED)));
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
                        LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 1))));

        assertEquals(before, auditEventRepository.count());
    }
}
