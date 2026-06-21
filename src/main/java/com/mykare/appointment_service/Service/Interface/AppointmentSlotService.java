package com.mykare.appointment_service.Service.Interface;

import com.mykare.appointment_service.DTO.Response.AvailableSlotsResponse;

import java.time.LocalDate;

public interface AppointmentSlotService {

    int generateSlotsForDate(LocalDate date);

    int generateTomorrowSlots();

    int generateSlotsTwoDaysAhead();

    AvailableSlotsResponse fetchAvailableSlots(LocalDate date);
}
