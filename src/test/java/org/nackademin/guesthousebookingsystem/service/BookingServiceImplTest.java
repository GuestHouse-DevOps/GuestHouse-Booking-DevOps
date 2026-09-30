package org.nackademin.guesthousebookingsystem.service;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.nackademin.guesthousebookingsystem.dto.BookingDto;
import org.nackademin.guesthousebookingsystem.dto.RoomDto;
import org.nackademin.guesthousebookingsystem.entity.Booking;
import org.nackademin.guesthousebookingsystem.entity.BookingStatus;
import org.nackademin.guesthousebookingsystem.entity.Room;
import org.nackademin.guesthousebookingsystem.entity.RoomType;
import org.nackademin.guesthousebookingsystem.repository.BookingRepository;
import org.nackademin.guesthousebookingsystem.repository.RoomRepository;
import jakarta.persistence.EntityManager;
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
class BookingServiceImplTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private EntityManager entityManager;

    @RegisterExtension
    static WireMockExtension customerService = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void customerServiceUrl(DynamicPropertyRegistry registry) {
        registry.add("customer.service.url", customerService::baseUrl);
    }

    private final Long customerId = 1L;
    private Room savedRoom;
    private Booking savedBooking;

    @BeforeEach
    void setUp() {
        customerService.stubFor(get(urlPathMatching("/api/customers/\\d+"))
                .willReturn(okJson("""
                        {"id": 1, "name": "Anna Andersson", "email": "anna@example.com", "phoneNumber": "0701234567"}
                        """)));

        Room room = new Room(null, 101, RoomType.DOUBLE, 1, false);
        savedRoom = roomRepository.save(room);

        Booking booking = new Booking(null, customerId, savedRoom,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 5),
                BookingStatus.CONFIRMED);
        savedBooking = bookingRepository.save(booking);
    }

    @Test
    void getAllBookings_shouldReturnList() {
        List<BookingDto> result = bookingService.getAllBookings();

        assertEquals(1, result.size());
        assertEquals(customerId, result.get(0).getCustomerId());
    }

    @Test
    void saveBooking_shouldSaveWhenDatesAreFree() {
        RoomDto roomDto = new RoomDto(savedRoom.getId(), 101, RoomType.DOUBLE, 1, false);

        BookingDto newBooking = new BookingDto(null, customerId, null, roomDto,
                LocalDate.of(2026, 6, 10),
                LocalDate.of(2026, 6, 15), null);

        BookingDto result = bookingService.saveBooking(newBooking);

        assertNotNull(result.getId());
        assertEquals(2, bookingRepository.findAll().size());
    }

    @Test
    void saveBooking_shouldThrowExceptionWhenDatesOverlap() {
        RoomDto roomDto = new RoomDto(savedRoom.getId(), 101, RoomType.DOUBLE, 1, false);

        BookingDto overlappingBooking = new BookingDto(null, customerId, null, roomDto,
                LocalDate.of(2026, 6, 3),
                LocalDate.of(2026, 6, 8), null);

        assertThrows(IllegalStateException.class, () -> bookingService.saveBooking(overlappingBooking));
    }

    @Test
    void saveBooking_shouldFailWhenCustomerDoesNotExist() {
        customerService.stubFor(get("/api/customers/99").willReturn(notFound()));
        RoomDto roomDto = new RoomDto(savedRoom.getId(), 101, RoomType.DOUBLE, 1, false);

        BookingDto booking = new BookingDto(null, 99L, null, roomDto,
                LocalDate.of(2026, 6, 10),
                LocalDate.of(2026, 6, 15), null);

        RuntimeException e = assertThrows(RuntimeException.class, () -> bookingService.saveBooking(booking));
        assertEquals("Kund med id 99 hittades inte", e.getMessage());
    }

    @Test
    void getBookingById_shouldIncludeCustomerNameFromCustomerService() {
        BookingDto result = bookingService.getBookingById(savedBooking.getId());
        assertEquals("Anna Andersson", result.getCustomerName());
    }

    @Test
    void saveBooking_shouldFailWhenCheckoutBeforeCheckin() {
        RoomDto roomDto = new RoomDto(savedRoom.getId(), 101, RoomType.DOUBLE, 1, false);

        BookingDto invalidBooking = new BookingDto(null, customerId, null, roomDto,
                LocalDate.of(2026, 6, 10),
                LocalDate.of(2026, 6, 5), null);

        assertThrows(IllegalArgumentException.class, () -> bookingService.saveBooking(invalidBooking));
    }

    @Test
    void getBookingById_shouldReturnBooking() {
        BookingDto result = bookingService.getBookingById(savedBooking.getId());
        assertEquals(customerId, result.getCustomerId());
    }

    @Test
    void updateBooking_shouldUpdateExistingBooking() {
        RoomDto roomDto = new RoomDto(savedRoom.getId(), 101, RoomType.DOUBLE, 1, false);

        BookingDto updateInfo = new BookingDto(null, customerId, null, roomDto,
                LocalDate.of(2026, 7, 2),
                LocalDate.of(2026, 7, 6), null);

        BookingDto result = bookingService.updateBooking(savedBooking.getId(), updateInfo);
        assertEquals(LocalDate.of(2026, 7, 2), result.getStartDate());
    }

    @Test
    void updateBooking_shouldKeepCreatedAtAndSetUpdatedAt() {
        entityManager.flush();
        entityManager.clear();
        Booking before = bookingRepository.findById(savedBooking.getId()).orElseThrow();
        assertNotNull(before.getCreatedAt());
        entityManager.clear();

        RoomDto roomDto = new RoomDto(savedRoom.getId(), 101, RoomType.DOUBLE, 1, false);
        BookingDto updateInfo = new BookingDto(null, customerId, null, roomDto,
                LocalDate.of(2026, 7, 2),
                LocalDate.of(2026, 7, 6), null);
        bookingService.updateBooking(savedBooking.getId(), updateInfo);
        entityManager.flush();
        entityManager.clear();

        Booking after = bookingRepository.findById(savedBooking.getId()).orElseThrow();
        assertEquals(before.getCreatedAt(), after.getCreatedAt());
        assertFalse(after.getUpdatedAt().isBefore(before.getUpdatedAt()));
    }

    @Test
    void cancelBooking_shouldKeepBookingAndReleaseRoom() {
        bookingService.cancelBooking(savedBooking.getId());

        assertEquals(BookingStatus.CANCELLED,
                bookingRepository.findById(savedBooking.getId()).orElseThrow().getStatus());
        assertFalse(bookingService.customerHasActiveBookings(customerId));

        RoomDto roomDto = new RoomDto(savedRoom.getId(), 101, RoomType.DOUBLE, 1, false);
        BookingDto sameDates = new BookingDto(null, customerId, null, roomDto,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 5), null);
        assertNotNull(bookingService.saveBooking(sameDates).getId());
    }

    @Test
    void updateBooking_checkOutShouldMarkRoomDirty() {
        RoomDto roomDto = new RoomDto(savedRoom.getId(), 101, RoomType.DOUBLE, 1, false);
        BookingDto checkOut = new BookingDto(null, customerId, null, roomDto,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 5), BookingStatus.CHECKED_OUT);

        bookingService.updateBooking(savedBooking.getId(), checkOut);

        assertTrue(roomRepository.findById(savedRoom.getId()).orElseThrow().isDirty());
    }

    @Test
    void saveBooking_shouldFailWhenRoomIsDirty() {
        savedRoom.setDirty(true);
        roomRepository.save(savedRoom);
        RoomDto roomDto = new RoomDto(savedRoom.getId(), 101, RoomType.DOUBLE, 1, true);
        BookingDto booking = new BookingDto(null, customerId, null, roomDto,
                LocalDate.of(2026, 6, 10),
                LocalDate.of(2026, 6, 15), null);

        assertThrows(IllegalStateException.class, () -> bookingService.saveBooking(booking));
    }
}
