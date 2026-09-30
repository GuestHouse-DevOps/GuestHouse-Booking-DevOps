package org.nackademin.guesthousebookingsystem.service;

import lombok.RequiredArgsConstructor;
import org.nackademin.guesthousebookingsystem.client.CustomerClient;
import org.nackademin.guesthousebookingsystem.dto.BookingDto;
import org.nackademin.guesthousebookingsystem.dto.CustomerDto;
import org.nackademin.guesthousebookingsystem.dto.RoomDto;
import org.nackademin.guesthousebookingsystem.entity.AuditEvent;
import org.nackademin.guesthousebookingsystem.entity.Booking;
import org.nackademin.guesthousebookingsystem.entity.BookingStatus;
import org.nackademin.guesthousebookingsystem.entity.Room;
import org.nackademin.guesthousebookingsystem.repository.AuditEventRepository;
import org.nackademin.guesthousebookingsystem.repository.BookingRepository;
import org.nackademin.guesthousebookingsystem.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final CustomerClient customerClient;
    private final AuditEventRepository auditEventRepository;

    private BookingDto toDto(Booking booking) {
        RoomDto roomDto = new RoomDto(
                booking.getRoom().getId(),
                booking.getRoom().getRoomNumber(),
                booking.getRoom().getRoomType(),
                booking.getRoom().getExtraBeds()
        );

        String customerName = "Kund-id: "
                + booking.getCustomerId();
        try {
            CustomerDto customer = customerClient
                    .getCustomerById(booking.getCustomerId());
            if (customer != null) {
                customerName = customer.getName();
            }
        } catch (RuntimeException e) {

        }

        return new BookingDto(
                booking.getId(),
                booking.getCustomerId(),
                customerName,
                roomDto,
                booking.getStartDate(),
                booking.getEndDate(),
                booking.getStatus()
        );
    }

    private Booking toEntity(BookingDto dto) {
        Room room = roomRepository
                .findById(dto.getRoom().getId())
                .orElseThrow(() ->
                        new RuntimeException("Rum hittades inte"));
        return new Booking(
                dto.getId(),
                dto.getCustomerId(),
                room,
                dto.getStartDate(),
                dto.getEndDate(),
                dto.getStatus() != null
                        ? dto.getStatus()
                        : BookingStatus.CONFIRMED
        );
    }

    private void checkConflicts(BookingDto dto, Long excludeId) {
        if (dto.getStartDate() == null
                || dto.getEndDate() == null) {
            throw new IllegalArgumentException("Datum saknas");
        }
        if (!dto.getEndDate().isAfter(dto.getStartDate())) {
            throw new IllegalArgumentException(
                    "Utcheckningsdatum måste vara "
                            + "efter incheckningsdatum");
        }
        if (dto.getStatus() == BookingStatus.CANCELLED) {
            return;
        }
        List<Booking> conflicts = bookingRepository.findOverlapping(
                dto.getRoom().getId(),
                dto.getStartDate(),
                dto.getEndDate(),
                excludeId
        );
        if (!conflicts.isEmpty()) {
            throw new IllegalStateException(
                    "Rummet är redan bokat för de valda datumen");
        }
    }

    @Override
    public List<BookingDto> getAllBookings() {
        return bookingRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public BookingDto getBookingById(Long id) {
        return bookingRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Bokning hittades inte"));
    }

    @Override
    @Transactional
    public BookingDto saveBooking(BookingDto bookingDto) {
        if (!customerClient.customerExists(
                bookingDto.getCustomerId())) {
            throw new RuntimeException(
                    "Kund med id "
                            + bookingDto.getCustomerId()
                            + " hittades inte");
        }
        checkConflicts(bookingDto, -1L);
        Booking saved = bookingRepository.save(
                toEntity(bookingDto));
        auditEventRepository.save(new AuditEvent(
                AuditEvent.Type.BOOKING, saved.getId(),
                AuditEvent.Action.CREATED));
        return toDto(saved);
    }

    @Override
    @Transactional
    public BookingDto updateBooking(Long id, BookingDto bookingDto) {
        if (!customerClient.customerExists(
                bookingDto.getCustomerId())) {
            throw new RuntimeException(
                    "Kund med id "
                            + bookingDto.getCustomerId()
                            + " hittades inte");
        }
        bookingDto.setId(id);
        checkConflicts(bookingDto, id);
        Booking saved = bookingRepository.save(
                toEntity(bookingDto));
        auditEventRepository.save(new AuditEvent(
                AuditEvent.Type.BOOKING, id,
                AuditEvent.Action.UPDATED));
        return toDto(saved);
    }

    @Override
    @Transactional
    public void cancelBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Bokning hittades inte"));
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        auditEventRepository.save(new AuditEvent(
                AuditEvent.Type.BOOKING, id,
                AuditEvent.Action.UPDATED));
    }

    @Override
    public boolean customerHasActiveBookings(Long customerId) {
        return bookingRepository.existsByCustomerIdAndStatusNot(
                customerId, BookingStatus.CANCELLED);
    }

    @Override
    public boolean customerHasBookedRoom(
            Long customerId,
            Long roomId) {
        return bookingRepository
                .existsByCustomerIdAndRoomId(customerId, roomId);
    }
}