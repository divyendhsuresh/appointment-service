package com.mykare.appointment_service.Entity;

import com.mykare.appointment_service.Enums.AppointmentStatus;
import com.mykare.appointment_service.Enums.NotificationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "appointments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_appointment_user")
    )
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "slot_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_appointment_slot")
    )
    private AppointmentSlot slot;

    @Column(name = "reason", length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AppointmentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "notification_status",
            nullable = false,
            length = 30
    )
    private NotificationStatus notificationStatus;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        if (id == null) {
            id = UUID.randomUUID();
        }

        if (status == null) {
            status = AppointmentStatus.CONFIRMED;
        }

        if (notificationStatus == null) {
            notificationStatus = NotificationStatus.PENDING;
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
