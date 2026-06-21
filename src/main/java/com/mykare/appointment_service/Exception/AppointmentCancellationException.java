package com.mykare.appointment_service.Exception;

public class AppointmentCancellationException extends RuntimeException {
    public AppointmentCancellationException(String message) {
        super(message);
    }
}