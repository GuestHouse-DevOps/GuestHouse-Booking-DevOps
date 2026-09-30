package org.nackademin.guesthousebookingsystem.service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nackademin.guesthousebookingsystem.dto.RoomDto;
import org.nackademin.guesthousebookingsystem.entity.Booking;
import org.nackademin.guesthousebookingsystem.entity.BookingStatus;
import org.nackademin.guesthousebookingsystem.entity.Room;
import org.nackademin.guesthousebookingsystem.entity.RoomType;
import org.nackademin.guesthousebookingsystem.repository.BookingRepository;
import org.nackademin.guesthousebookingsystem.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles({"test", "local"})
@Transactional
class RoomServiceImplTest {

    @Autowired
    private RoomService roomService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BookingRepository bookingRepository;

    private Room savedRoom;

    @BeforeEach
    void setUp() {
        Room room = new Room(null, 101, RoomType.DOUBLE, 1, false);
        savedRoom = roomRepository.save(room);
    }

    @Test
    void getAllRooms_shouldReturnList() {
        List<RoomDto> result = roomService.getAllRooms();

        assertEquals(1, result.size());
        assertEquals(101, result.get(0).getRoomNumber());
    }

    @Test
    void getRoomById_shouldReturnRoom() {
        RoomDto result = roomService.getRoomById(savedRoom.getId());

        assertEquals(101, result.getRoomNumber());
        assertEquals(RoomType.DOUBLE, result.getRoomType());
    }

    @Test
    void saveRoom_shouldReturnSavedRoom() {
        RoomDto newRoom = new RoomDto(null, 102, RoomType.SINGLE, 0, false);

        RoomDto result = roomService.saveRoom(newRoom);

        assertEquals(102, result.getRoomNumber());
        assertEquals(2, roomRepository.findAll().size());
    }

    @Test
    void findAvailableRooms_shouldReturnAvailableRooms() {
        List<RoomDto> result = roomService.findAvailableRooms(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 5),
                1);
        assertFalse(result.isEmpty());
    }

    @Test
    void saveRoom_shouldThrowWhenRoomNumberBelowOne() {
        RoomDto invalidRoom = new RoomDto(null, 0, RoomType.SINGLE, 0, false);
        assertThrows(IllegalArgumentException.class, () -> roomService.saveRoom(invalidRoom));
    }

    @Test
    void saveRoom_shouldThrowWhenRoomNumberAlreadyExists() {
        RoomDto duplicate = new RoomDto(null, 101, RoomType.SINGLE, 0, false);
        assertThrows(IllegalArgumentException.class, () -> roomService.saveRoom(duplicate));
    }

    @Test
    void deleteRoom_shouldFailIfRoomHasActiveBookings() {
        Long customerId = 1L;
        Booking booking = new Booking(null, customerId, savedRoom,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 5),
                BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
        assertThrows(IllegalStateException.class, () -> roomService.deleteRoom(savedRoom.getId()));
    }

    @Test
    void updateRoom_shouldUpdateExistingRoom() {
        RoomDto updateInfo = new RoomDto(null, 101, RoomType.DOUBLE, 2, false);

        RoomDto result = roomService.updateRoom(savedRoom.getId(), updateInfo);

        assertEquals(2, result.getExtraBeds());
    }

    @Test
    void deleteRoom_shouldDeleteWhenNoBookings() {
        roomService.deleteRoom(savedRoom.getId());

        assertEquals(0, roomRepository.findAll().size());
    }

    @Test
    void findAvailableRooms_shouldSkipDirtyRooms() {
        savedRoom.setDirty(true);
        roomRepository.save(savedRoom);

        List<RoomDto> result = roomService.findAvailableRooms(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 5),
                1);
        assertTrue(result.isEmpty());
    }
}
