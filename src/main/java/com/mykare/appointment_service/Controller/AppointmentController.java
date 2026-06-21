package com.mykare.appointment_service.Controller;

import com.mykare.appointment_service.Common.Response.ApiResponse;
import com.mykare.appointment_service.Config.OpenApiConfig;
import com.mykare.appointment_service.DTO.Request.CreateAppointmentRequest;
import com.mykare.appointment_service.DTO.Response.CancelAppointmentResponse;
import com.mykare.appointment_service.DTO.Response.CreateAppointmentResponse;
import com.mykare.appointment_service.DTO.Response.UserAppointmentsResponse;
import com.mykare.appointment_service.Service.Interface.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(
        name = "Appointments",
        description = "Appointment booking, retrieval and cancellation APIs"
)
@SecurityRequirement(name = OpenApiConfig.JWT_SECURITY_SCHEME)
public class AppointmentController {

    private final AppointmentService appointmentService;

    @Operation(
            summary = "Book an appointment",
            description = """
                    Books an available appointment slot.

                    The API locks the selected slot, prevents double booking,
                    creates appointment history and publishes a Kafka notification event.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Appointment booked successfully"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "JWT token is missing or invalid"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Appointment slot not found"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Appointment slot is unavailable"
    )
    @PostMapping
    public ResponseEntity<ApiResponse<CreateAppointmentResponse>> createAppointment(
            @Valid
            @RequestBody CreateAppointmentRequest request,
            Authentication authentication
    ) {

        CreateAppointmentResponse response =
                appointmentService.createAppointment(authentication.getName(), request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.of(
                                HttpStatus.CREATED.value(),
                                "Appointment booked successfully",
                                response
                        )
                );
    }

    @Operation(
            summary = "Fetch logged-in user's appointments",
            description = "Returns appointments belonging to the authenticated user"
    )
    @GetMapping
    public ResponseEntity<ApiResponse<UserAppointmentsResponse>> fetchUserAppointments(
            Authentication authentication
    ) {

        UserAppointmentsResponse response =
                appointmentService.fetchUserAppointments(
                        authentication.getName()
                );

        String message =
                response.totalAppointments() == 0
                        ? "No appointments found"
                        : "Appointments fetched successfully";

        return ResponseEntity.ok(
                ApiResponse.of(
                        HttpStatus.OK.value(),
                        message,
                        response
                )
        );
    }

    @Operation(
            summary = "Cancel appointment",
            description = """
                    Cancels a confirmed appointment belonging to the authenticated user
                    and releases the appointment slot.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Appointment cancelled successfully"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Appointment belongs to another user"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Appointment not found"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Appointment cannot be cancelled"
    )
    @PatchMapping("/{appointmentId}/cancel")
    public ResponseEntity<ApiResponse<CancelAppointmentResponse>> cancelAppointment(
            @PathVariable UUID appointmentId,
            Authentication authentication
    ) {

        CancelAppointmentResponse response =
                appointmentService.cancelAppointment(authentication.getName(), appointmentId);

        return ResponseEntity.ok(
                ApiResponse.of(
                        HttpStatus.OK.value(),
                        "Appointment cancelled successfully",
                        response
                )
        );
    }
}