package org.nackademin.guesthousebookingsystem.service;

import org.nackademin.guesthousebookingsystem.dto.BookingDto;

import java.util.List;

public interface BookingService {

    List<BookingDto> getAllBookings();

    BookingDto getBookingById(Long id);

    BookingDto saveBooking(BookingDto bookingDto);

    BookingDto updateBooking(Long id, BookingDto bookingDto);

    void cancelBooking(Long id);

    boolean customerHasActiveBookings(Long customerId);

    boolean customerHasBookedRoom(Long customerId, Long roomId);
}