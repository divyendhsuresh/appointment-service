package com.mykare.appointment_service.Common.Response;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record ApiResponse<T>(
        StatusResponse status,
        T data
) {

    public static <T> ApiResponse<T> of(
            int code,
            String message,
            T data
    ) {
        StatusResponse statusResponse = new StatusResponse(
                code,
                message,
                OffsetDateTime.now(ZoneOffset.UTC)
        );

        return new ApiResponse<>(statusResponse, data);
    }
}