package com.mykare.appointment_service.Entity;

import com.mykare.appointment_service.Enums.AppointmentHistoryAction;
import com.mykare.appointment_service.Enums.AppointmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "appointment_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentHistory {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "appointment_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_history_appointment")
    )
    private Appointment appointment;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    private AppointmentHistoryAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 30)
    private AppointmentStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 30)
    private AppointmentStatus newStatus;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "changed_by")
    private UUID changedBy;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }

        createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}