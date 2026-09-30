package org.nackademin.guesthousebookingsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nackademin.guesthousebookingsystem.entity.RoomType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomDto {

    private Long id;
    private int roomNumber;
    private RoomType roomType;
    private int extraBeds;
    private boolean dirty;
}