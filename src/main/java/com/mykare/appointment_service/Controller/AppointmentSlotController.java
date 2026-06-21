package com.mykare.appointment_service.Controller;

import com.mykare.appointment_service.Common.Response.ApiResponse;
import com.mykare.appointment_service.DTO.Response.AvailableSlotsResponse;
import com.mykare.appointment_service.Service.Interface.AppointmentSlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/slots")
@RequiredArgsConstructor
public class AppointmentSlotController {

    private final AppointmentSlotService slotService;

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<AvailableSlotsResponse>>
    fetchAvailableSlots(

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {

        AvailableSlotsResponse response =
                slotService.fetchAvailableSlots(date);

        String message;

        if (response.totalAvailableSlots() == 0) {
            message = "No available slots found";
        } else {
            message = "Available slots fetched successfully";
        }

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        ApiResponse.of(
                                HttpStatus.OK.value(),
                                message,
                                response
                        )
                );
    }
}
