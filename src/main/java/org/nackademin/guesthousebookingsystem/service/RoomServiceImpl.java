package org.nackademin.guesthousebookingsystem.service;

import lombok.RequiredArgsConstructor;
import org.nackademin.guesthousebookingsystem.dto.RoomDto;
import org.nackademin.guesthousebookingsystem.entity.AuditEvent;
import org.nackademin.guesthousebookingsystem.entity.Room;
import org.nackademin.guesthousebookingsystem.repository.AuditEventRepository;
import org.nackademin.guesthousebookingsystem.repository.BookingRepository;
import org.nackademin.guesthousebookingsystem.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final AuditEventRepository auditEventRepository;

    private RoomDto toDto(Room room) {
        return new RoomDto(room.getId(), room.getRoomNumber(), room.getRoomType(), room.getExtraBeds());
    }

    private Room toEntity(RoomDto dto) {
        return new Room(dto.getId(), dto.getRoomNumber(), dto.getRoomType(), dto.getExtraBeds());
    }

    @Override
    public List<RoomDto> getAllRooms() {
        return roomRepository.findAll().stream().map(this::toDto).toList();
    }

    @Override
    public RoomDto getRoomById(Long id) {
        return roomRepository.findById(id).map(this::toDto).orElseThrow(() -> new RuntimeException("Rum hittades inte"));
    }

    @Override
    @Transactional
    public RoomDto saveRoom(RoomDto roomDto) {
        if (roomDto.getRoomNumber() < 1) {
            throw new IllegalArgumentException("Rumsnummer måste vara större än 0.");
        }
        if (roomRepository.existsByRoomNumber(roomDto.getRoomNumber())) {
            throw new IllegalArgumentException("Rumsnummer finns redan.");
        }
        Room saved = roomRepository.save(toEntity(roomDto));
        auditEventRepository.save(new AuditEvent(AuditEvent.Type.ROOM, saved.getId(), AuditEvent.Action.CREATED));
        return toDto(saved);
    }

    @Override
    @Transactional
    public RoomDto updateRoom(Long id, RoomDto roomDto) {
        roomDto.setId(id);
        Room saved = roomRepository.save(toEntity(roomDto));
        auditEventRepository.save(new AuditEvent(AuditEvent.Type.ROOM, id, AuditEvent.Action.UPDATED));
        return toDto(saved);
    }

    @Override
    @Transactional
    public void deleteRoom(Long id) {
        Room room = roomRepository.findById(id).orElseThrow(() -> new RuntimeException("Rum hittades inte"));
        if (!bookingRepository.findByRoom(room).isEmpty()) {
            throw new IllegalStateException("Kan inte ta bort rum! Det finns aktiva bokningar kopplade till rummet");
        }
        roomRepository.deleteById(id);
        auditEventRepository.save(new AuditEvent(AuditEvent.Type.ROOM, id, AuditEvent.Action.DELETED));
    }

    @Override
    public List<RoomDto> findAvailableRooms(LocalDate startDate, LocalDate endDate, int guests) {
        return roomRepository.findAvailableRooms(startDate, endDate, guests).stream().map(this::toDto).toList();
    }
}