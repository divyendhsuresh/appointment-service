package com.mykare.appointment_service.Exception;

public class AppointmentAccessDeniedException extends RuntimeException {
    public AppointmentAccessDeniedException(String message) {
        super(message);
    }
}
