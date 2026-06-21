package com.mykare.appointment_service.Controller;

import com.mykare.appointment_service.Common.Response.ApiResponse;
import com.mykare.appointment_service.DTO.Request.CreateAppointmentRequest;
import com.mykare.appointment_service.DTO.Response.CancelAppointmentResponse;
import com.mykare.appointment_service.DTO.Response.CreateAppointmentResponse;
import com.mykare.appointment_service.DTO.Response.UserAppointmentsResponse;
import com.mykare.appointment_service.Service.Interface.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    public ResponseEntity<ApiResponse<CreateAppointmentResponse>> createAppointment(@Valid @RequestBody CreateAppointmentRequest request, Authentication authentication) {

        CreateAppointmentResponse response = appointmentService.createAppointment(authentication.getName(), request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.of(HttpStatus.CREATED.value(), "Appointment booked successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<UserAppointmentsResponse>> fetchUserAppointments(Authentication authentication) {

        UserAppointmentsResponse response = appointmentService.fetchUserAppointments(authentication.getName());

        String message = response.totalAppointments() == 0
                ? "No appointments found"
                : "Appointments fetched successfully";

        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK.value(), message, response));
    }

    @PatchMapping("/{appointmentId}/cancel")
    public ResponseEntity<ApiResponse<CancelAppointmentResponse>> cancelAppointment(@PathVariable UUID appointmentId, Authentication authentication) {

        CancelAppointmentResponse response = appointmentService.cancelAppointment(authentication.getName(), appointmentId);

        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK.value(), "Appointment cancelled successfully", response));}
}
