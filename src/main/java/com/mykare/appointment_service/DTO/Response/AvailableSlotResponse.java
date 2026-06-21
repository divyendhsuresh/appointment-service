package com.mykare.appointment_service.DTO.Response;

import com.mykare.appointment_service.Enums.SlotStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AvailableSlotResponse(
        UUID slotId,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        SlotStatus status
) {
}