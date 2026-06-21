package com.mykare.appointment_service.Controller;

import com.mykare.appointment_service.Common.Response.ApiResponse;
import com.mykare.appointment_service.Config.OpenApiConfig;
import com.mykare.appointment_service.DTO.Response.AvailableSlotsResponse;
import com.mykare.appointment_service.Service.Interface.AppointmentSlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/slots")
@RequiredArgsConstructor
@Tag(
        name = "Appointment Slots",
        description = "Available appointment slot APIs")
@SecurityRequirement(name = OpenApiConfig.JWT_SECURITY_SCHEME)
public class AppointmentSlotController {

    private final AppointmentSlotService slotService;

    @Operation(
            summary = "Fetch available slots",
            description = "Returns only AVAILABLE slots for the selected date"
    )
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<AvailableSlotsResponse>> fetchAvailableSlots(
            @Parameter(
                    description = "Appointment date in YYYY-MM-DD format",
                    example = "2026-06-23",
                    required = true
            )
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {

        AvailableSlotsResponse response = slotService.fetchAvailableSlots(date);

        String message =
                response.totalAvailableSlots() == 0
                        ? "No available slots found"
                        : "Available slots fetched successfully";

        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK.value(), message, response));
    }
}