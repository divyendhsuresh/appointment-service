package com.mykare.appointment_service.DTO.Response;

import com.mykare.appointment_service.Enums.UserRole;

import java.time.OffsetDateTime;
import java.util.UUID;

public record LoginResponse(
        UUID userId,
        String fullName,
        String email,
        UserRole role,
        String accessToken,
        String tokenType,
        OffsetDateTime expiresAt)
{ }
