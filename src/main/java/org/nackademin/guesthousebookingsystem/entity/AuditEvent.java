package org.nackademin.guesthousebookingsystem.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Data
@NoArgsConstructor
public class AuditEvent {

    public enum Type { BOOKING, ROOM, CUSTOMER }

    public enum Action { CREATED, UPDATED, DELETED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Type entityType;

    private Long entityId;

    @Enumerated(EnumType.STRING)
    private Action action;

    @CreationTimestamp
    private Instant occurredAt;

    public AuditEvent(Type entityType, Long entityId, Action action) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
    }
}
