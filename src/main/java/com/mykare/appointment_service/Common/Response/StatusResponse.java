package com.mykare.appointment_service.Common.Response;

import java.time.OffsetDateTime;

public record StatusResponse(
        int code,
        String message,
        OffsetDateTime timestamp)
{ }
