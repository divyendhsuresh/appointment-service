package com.mykare.appointment_service.Service.Interface;

import com.mykare.appointment_service.DTO.Request.CreateAppointmentRequest;
import com.mykare.appointment_service.DTO.Response.CancelAppointmentResponse;
import com.mykare.appointment_service.DTO.Response.CreateAppointmentResponse;
import com.mykare.appointment_service.DTO.Response.UserAppointmentsResponse;

import java.util.UUID;

public interface AppointmentService {
    CreateAppointmentResponse createAppointment(String userEmail, CreateAppointmentRequest request);
    UserAppointmentsResponse fetchUserAppointments(String userEmail);
    CancelAppointmentResponse cancelAppointment(String userEmail, UUID appointmentId);
}
