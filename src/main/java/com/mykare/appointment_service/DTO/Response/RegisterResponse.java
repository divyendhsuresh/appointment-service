package com.mykare.appointment_service.DTO.Response;

import com.mykare.appointment_service.Enums.UserRole;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RegisterResponse(

        UUID userId,

        String fullName,

        String email,

        String phone,

        UserRole role,

        OffsetDateTime createdAt)
{ }
