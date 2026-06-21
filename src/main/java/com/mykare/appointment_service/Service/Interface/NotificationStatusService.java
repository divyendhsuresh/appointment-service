package com.mykare.appointment_service.Service.Interface;

import com.mykare.appointment_service.DTO.Response.NotificationStatusResponse;
import com.mykare.appointment_service.Enums.NotificationStatus;

import java.util.UUID;

public interface NotificationStatusService {

    NotificationStatusResponse updateStatus(
            UUID appointmentId,
            NotificationStatus status
    );

    void markFailed(UUID appointmentId);
}