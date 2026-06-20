package com.mykare.appointment_service.Common.Response;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record ApiResponse<T>(
        StatusResponse status,
        T data
) {
    public static <T> ApiResponse<T> success(
            String code,
            String message,
            T data
    ) {
        return new ApiResponse<>(
                new StatusResponse(
                        code,
                        message,
                        OffsetDateTime.now(ZoneOffset.UTC)
                ),
                data
        );
    }
    public static <T> ApiResponse<T> error(
            String code,
            String message,
            T data
    ) {
        return new ApiResponse<>(
                new StatusResponse(
                        code,
                        message,
                        OffsetDateTime.now(ZoneOffset.UTC)
                ),
                data
        );
    }
}
