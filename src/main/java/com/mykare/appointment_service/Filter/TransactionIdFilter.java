package com.mykare.appointment_service.Filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mykare.appointment_service.Common.Constants.HeaderConstants;
import com.mykare.appointment_service.Common.Response.ApiResponse;
import com.mykare.appointment_service.DTO.Response.ErrorResponseData;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class TransactionIdFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String transactionId = request.getHeader(HeaderConstants.TRANSACTION_ID);

        if (transactionId == null || transactionId.isBlank()) {

            ErrorResponseData errorData =
                    new ErrorResponseData(
                            "X-Transaction-Id header is required",
                            null
                    );

            ApiResponse<ErrorResponseData> apiResponse =
                    ApiResponse.of(
                            HttpStatus.BAD_REQUEST.value(),
                            "Missing transaction ID",
                            errorData
                    );

            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            objectMapper.writeValue(response.getOutputStream(), apiResponse);
            return;
        }

        try {
            MDC.put(HeaderConstants.MDC_TRANSACTION_ID, transactionId);

            response.setHeader(HeaderConstants.TRANSACTION_ID, transactionId);

            filterChain.doFilter(request, response);

        } finally {
            MDC.remove(HeaderConstants.MDC_TRANSACTION_ID);
        }
    }

    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request
    ) {

        /*
         * Do not require the header for browser CORS
         * preflight requests.
         */
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }
}