package com.mykare.appointment_service.DTO.Request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateAppointmentRequest(

        @NotNull(message = "Slot ID is required")
        UUID slotId,

        @Size(max = 500, message = "Reason cannot exceed 500 characters")
        String reason)
{ }
