package com.mykare.appointment_service.DTO.Response;
import java.util.List;

public record UserAppointmentsResponse(
        int totalAppointments,
        List<UserAppointmentResponse> appointments
) {
}
