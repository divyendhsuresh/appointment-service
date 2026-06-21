package com.mykare.appointment_service.DTO.Response;

import com.mykare.appointment_service.Enums.AppointmentStatus;
import com.mykare.appointment_service.Enums.SlotStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CancelAppointmentResponse(
        UUID appointmentId,
        UUID slotId,
        AppointmentStatus appointmentStatus,
        SlotStatus slotStatus,
        OffsetDateTime cancelledAt
) {
}
