package org.nackademin.guesthousebookingsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nackademin.guesthousebookingsystem.entity.BookingStatus;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingDto {

    private Long id;
    private Long customerId;
    private String customerName;
    private RoomDto room;
    private LocalDate startDate;
    private LocalDate endDate;
    private BookingStatus status;
}