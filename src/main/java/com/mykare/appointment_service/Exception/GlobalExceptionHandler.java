package com.mykare.appointment_service.Exception;

import com.mykare.appointment_service.Common.Constants.HeaderConstants;
import com.mykare.appointment_service.Common.Response.ApiResponse;
import com.mykare.appointment_service.DTO.Response.ErrorResponseData;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<ErrorResponseData>>
    handleEmailAlreadyExists(
            EmailAlreadyExistsException exception,
            HttpServletRequest request
    ) {

        log.warn(
                "Email already exists. method={}, path={}, transactionId={}, message={}",
                request.getMethod(),
                request.getRequestURI(),
                getTransactionId(),
                exception.getMessage()
        );

        return buildErrorResponse(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                null
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ErrorResponseData>>
    handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {

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

        log.warn(
                "Request validation failed. method={}, path={}, transactionId={}, validationErrors={}",
                request.getMethod(),
                request.getRequestURI(),
                getTransactionId(),
                validationErrors
        );

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                validationErrors
        );
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<ErrorResponseData>>
    handleInvalidCredentials(
            InvalidCredentialsException exception,
            HttpServletRequest request
    ) {

        log.warn(
                "Invalid login credentials. method={}, path={}, transactionId={}",
                request.getMethod(),
                request.getRequestURI(),
                getTransactionId()
        );

        return buildErrorResponse(
                HttpStatus.UNAUTHORIZED,
                exception.getMessage(),
                null
        );
    }

    @ExceptionHandler(UserInactiveException.class)
    public ResponseEntity<ApiResponse<ErrorResponseData>>
    handleInactiveUser(
            UserInactiveException exception,
            HttpServletRequest request
    ) {

        log.warn(
                "Inactive user attempted an operation. method={}, path={}, transactionId={}, message={}",
                request.getMethod(),
                request.getRequestURI(),
                getTransactionId(),
                exception.getMessage()
        );

        return buildErrorResponse(
                HttpStatus.FORBIDDEN,
                exception.getMessage(),
                null
        );
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiResponse<ErrorResponseData>>
    handleUsernameNotFound(
            UsernameNotFoundException exception,
            HttpServletRequest request
    ) {

        log.warn(
                "Authenticated user not found. method={}, path={}, transactionId={}, message={}",
                request.getMethod(),
                request.getRequestURI(),
                getTransactionId(),
                exception.getMessage()
        );

        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                null
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<ErrorResponseData>>
    handleIllegalArgumentException(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {

        log.warn(
                "Illegal argument received. method={}, path={}, transactionId={}, message={}",
                request.getMethod(),
                request.getRequestURI(),
                getTransactionId(),
                exception.getMessage()
        );

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                null
        );
    }

    @ExceptionHandler(SlotNotFoundException.class)
    public ResponseEntity<ApiResponse<ErrorResponseData>>
    handleSlotNotFoundException(
            SlotNotFoundException exception,
            HttpServletRequest request
    ) {

        log.warn(
                "Appointment slot not found. method={}, path={}, transactionId={}, message={}",
                request.getMethod(),
                request.getRequestURI(),
                getTransactionId(),
                exception.getMessage()
        );

        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                null
        );
    }

    @ExceptionHandler(SlotUnavailableException.class)
    public ResponseEntity<ApiResponse<ErrorResponseData>>
    handleSlotUnavailableException(
            SlotUnavailableException exception,
            HttpServletRequest request
    ) {

        log.warn(
                "Appointment slot unavailable. method={}, path={}, transactionId={}, message={}",
                request.getMethod(),
                request.getRequestURI(),
                getTransactionId(),
                exception.getMessage()
        );

        return buildErrorResponse(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                null
        );
    }

    @ExceptionHandler(AppointmentNotFoundException.class)
    public ResponseEntity<ApiResponse<ErrorResponseData>>
    handleAppointmentNotFoundException(
            AppointmentNotFoundException exception,
            HttpServletRequest request
    ) {

        log.warn(
                "Appointment not found. method={}, path={}, transactionId={}, message={}",
                request.getMethod(),
                request.getRequestURI(),
                getTransactionId(),
                exception.getMessage()
        );

        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                null
        );
    }

    @ExceptionHandler(AppointmentAccessDeniedException.class)
    public ResponseEntity<ApiResponse<ErrorResponseData>>
    handleAppointmentAccessDeniedException(
            AppointmentAccessDeniedException exception,
            HttpServletRequest request
    ) {

        log.warn(
                "Appointment access denied. method={}, path={}, transactionId={}, message={}",
                request.getMethod(),
                request.getRequestURI(),
                getTransactionId(),
                exception.getMessage()
        );

        return buildErrorResponse(
                HttpStatus.FORBIDDEN,
                exception.getMessage(),
                null
        );
    }

    @ExceptionHandler(AppointmentCancellationException.class)
    public ResponseEntity<ApiResponse<ErrorResponseData>>
    handleAppointmentCancellationException(
            AppointmentCancellationException exception,
            HttpServletRequest request
    ) {

        log.warn(
                "Appointment cancellation rejected. method={}, path={}, transactionId={}, message={}",
                request.getMethod(),
                request.getRequestURI(),
                getTransactionId(),
                exception.getMessage()
        );

        return buildErrorResponse(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                null
        );
    }

    /*
     * Keep this handler last because it catches every exception
     * not handled by the methods above.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ErrorResponseData>>
    handleGenericException(
            Exception exception,
            HttpServletRequest request
    ) {

        log.error(
                "Unexpected application error. method={}, path={}, transactionId={}, exceptionType={}",
                request.getMethod(),
                request.getRequestURI(),
                getTransactionId(),
                exception.getClass().getSimpleName(),
                exception
        );

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",
                null
        );
    }

    private ResponseEntity<ApiResponse<ErrorResponseData>>
    buildErrorResponse(
            HttpStatus status,
            String message,
            Map<String, String> validationErrors
    ) {

        ErrorResponseData errorData =
                new ErrorResponseData(
                        message,
                        validationErrors
                );

        return ResponseEntity
                .status(status)
                .body(
                        ApiResponse.of(
                                status.value(),
                                message,
                                errorData
                        )
                );
    }

    private String getTransactionId() {

        String transactionId =
                MDC.get(
                        HeaderConstants.MDC_TRANSACTION_ID
                );

        return transactionId != null
                ? transactionId
                : "N/A";
    }
}