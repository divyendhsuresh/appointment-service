package com.mykare.appointment_service.Service.Interface;

import com.mykare.appointment_service.DTO.Request.CreateAppointmentRequest;
import com.mykare.appointment_service.DTO.Response.CreateAppointmentResponse;

public interface AppointmentService {
    CreateAppointmentResponse createAppointment(String userEmail, CreateAppointmentRequest request);
}
