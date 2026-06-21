package com.mykare.appointment_service.DTO.Request;

import com.mykare.appointment_service.Enums.NotificationStatus;
import jakarta.validation.constraints.NotNull;

public record NotificationStatusUpdateRequest(

        @NotNull(message = "Notification status is required")
        NotificationStatus status
) {
}