package com.mykare.appointment_service.Exception;

public class UserInactiveException extends RuntimeException {

    public UserInactiveException(String message) {
        super(message);
    }
}