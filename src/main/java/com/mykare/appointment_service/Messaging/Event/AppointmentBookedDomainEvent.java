package com.mykare.appointment_service.Messaging.Event;

public record AppointmentBookedDomainEvent(
        AppointmentNotificationEvent notificationEvent
) {
}