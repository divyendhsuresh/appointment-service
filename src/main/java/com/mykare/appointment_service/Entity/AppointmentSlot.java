package com.mykare.appointment_service.Entity;

import com.mykare.appointment_service.Enums.SlotStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(
        name = "appointment_slots",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_appointment_slot_time",
                        columnNames = {"start_time", "end_time"}
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentSlot {

    @Id
    @Column(
            name = "id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @Column(
            name = "start_time",
            nullable = false
    )
    private OffsetDateTime startTime;

    @Column(
            name = "end_time",
            nullable = false
    )
    private OffsetDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private SlotStatus status;

    @Version
    @Column(
            name = "version",
            nullable = false
    )
    private Long version;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {

        OffsetDateTime now =
                OffsetDateTime.now(ZoneOffset.UTC);

        if (id == null) {
            id = UUID.randomUUID();
        }

        if (status == null) {
            status = SlotStatus.AVAILABLE;
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
