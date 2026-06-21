package com.mykare.appointment_service.Controller;

import com.mykare.appointment_service.Common.Response.ApiResponse;
import com.mykare.appointment_service.DTO.Request.NotificationStatusUpdateRequest;
import com.mykare.appointment_service.DTO.Response.NotificationStatusResponse;
import com.mykare.appointment_service.Service.Interface.NotificationStatusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/appointments")
@RequiredArgsConstructor
public class InternalNotificationController {

    private final NotificationStatusService notificationStatusService;

    @Value("${app.internal.api-key}")
    private String configuredApiKey;

    @PatchMapping("/{appointmentId}/notification-status")
    public ResponseEntity<ApiResponse<NotificationStatusResponse>>
    updateNotificationStatus(

            @PathVariable UUID appointmentId,

            @RequestHeader("X-Internal-Api-Key")
            String providedApiKey,

            @Valid
            @RequestBody
            NotificationStatusUpdateRequest request
    ) {

        if (!configuredApiKey.equals(providedApiKey)) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(
                            ApiResponse.of(
                                    HttpStatus.UNAUTHORIZED.value(),
                                    "Invalid internal API key",
                                    null
                            )
                    );
        }

        NotificationStatusResponse response =
                notificationStatusService.updateStatus(
                        appointmentId,
                        request.status()
                );

        return ResponseEntity.ok(
                ApiResponse.of(
                        HttpStatus.OK.value(),
                        "Notification status updated successfully",
                        response
                )
        );
    }
}