package com.mykare.appointment_service.Exception;

import com.mykare.appointment_service.Common.Response.ApiResponse;
import com.mykare.appointment_service.DTO.Response.ErrorResponseData;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<ErrorResponseData>>
    handleEmailAlreadyExists(EmailAlreadyExistsException exception) {
        ErrorResponseData errorData = new ErrorResponseData(
                exception.getMessage(),
                null);
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.of(HttpStatus.CONFLICT.value(), exception.getMessage(), errorData));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ErrorResponseData>>
    handleValidationException(MethodArgumentNotValidException exception) {
        Map<String, String> validationErrors =
                new LinkedHashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        validationErrors.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        ErrorResponseData errorData = new ErrorResponseData(
                "Request validation failed",
                validationErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.of(HttpStatus.BAD_REQUEST.value(), "Request validation failed", errorData));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ErrorResponseData>>
    handleGenericException(Exception exception) {
        ErrorResponseData errorData = new ErrorResponseData("An unexpected error occurred", null);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "An unexpected error occurred", errorData));
    }
}
