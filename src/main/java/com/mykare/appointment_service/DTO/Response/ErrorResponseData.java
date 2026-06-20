package com.mykare.appointment_service.DTO.Response;

import java.util.Map;

public record ErrorResponseData(
        String error,
        Map<String, String> validationErrors) {
}