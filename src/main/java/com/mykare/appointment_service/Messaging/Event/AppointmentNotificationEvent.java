package com.mykare.appointment_service.Messaging.Event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AppointmentNotificationEvent(
        UUID eventId,
        String transactionId,
        UUID appointmentId,
        UUID userId,
        String email,
        String fullName,
        String phone,
        UUID slotId,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        String reason,
        String eventType,
        OffsetDateTime createdAt
) {
}