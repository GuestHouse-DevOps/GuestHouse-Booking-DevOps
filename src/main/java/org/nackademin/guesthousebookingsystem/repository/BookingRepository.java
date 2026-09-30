package org.nackademin.guesthousebookingsystem.repository;

import org.nackademin.guesthousebookingsystem.entity.Booking;
import org.nackademin.guesthousebookingsystem.entity.BookingStatus;
import org.nackademin.guesthousebookingsystem.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository
        extends JpaRepository<Booking, Long> {

    List<Booking> findByRoom(Room room);

    List<Booking> findByCustomerId(Long customerId);

    boolean existsByCustomerIdAndStatusNot(
            Long customerId,
            BookingStatus status);

    boolean existsByCustomerIdAndRoomId(
            Long customerId,
            Long roomId);

    @Query("""
            SELECT b FROM Booking b
            WHERE b.room.id   = :roomId
            AND b.startDate   < :endDate
            AND b.endDate     > :startDate
            AND b.id         != :excludeId
            AND b.status     != org.nackademin.guesthousebookingsystem.entity.BookingStatus.CANCELLED
            """)
    List<Booking> findOverlapping(
            @Param("roomId")    Long roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate")   LocalDate endDate,
            @Param("excludeId") Long excludeId
    );
}