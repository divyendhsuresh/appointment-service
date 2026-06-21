package com.mykare.appointment_service.DTO.Response;

import java.time.LocalDate;
import java.util.List;

public record AvailableSlotsResponse(
        LocalDate date,
        String timezone,
        int totalAvailableSlots,
        List<AvailableSlotResponse> slots
) {
}