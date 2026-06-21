package com.mykare.appointment_service.DTO.Response;

import java.time.OffsetDateTime;

public record LogoutResponse(
        String email,
        OffsetDateTime loggedOutAt
) {
}