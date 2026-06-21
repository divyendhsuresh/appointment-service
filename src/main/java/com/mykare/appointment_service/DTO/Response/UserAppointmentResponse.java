package com.mykare.appointment_service.DTO.Response;

import com.mykare.appointment_service.Enums.AppointmentStatus;
import com.mykare.appointment_service.Enums.NotificationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserAppointmentResponse(
        UUID appointmentId,
        UUID slotId,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        String reason,
        AppointmentStatus status,
        NotificationStatus notificationStatus,
        OffsetDateTime cancelledAt,
        OffsetDateTime createdAt
) {
}