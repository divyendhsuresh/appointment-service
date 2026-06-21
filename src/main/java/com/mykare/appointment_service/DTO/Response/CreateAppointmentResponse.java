package com.mykare.appointment_service.DTO.Response;

import com.mykare.appointment_service.Enums.AppointmentStatus;
import com.mykare.appointment_service.Enums.NotificationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateAppointmentResponse(
        UUID appointmentId,
        UUID slotId,
        UUID userId,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        String reason,
        AppointmentStatus status,
        NotificationStatus notificationStatus,
        OffsetDateTime createdAt)
{ }
