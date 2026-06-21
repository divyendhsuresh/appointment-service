package com.mykare.appointment_service.DTO.Response;

import com.mykare.appointment_service.Enums.NotificationStatus;

import java.util.UUID;

public record NotificationStatusResponse(
        UUID appointmentId,
        NotificationStatus notificationStatus
) {
}