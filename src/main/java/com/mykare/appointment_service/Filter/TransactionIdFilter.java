package com.mykare.appointment_service.Filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mykare.appointment_service.Common.Response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionIdFilter extends OncePerRequestFilter {

    public static final String TRANSACTION_ID_HEADER = "X-Transaction-Id";
    public static final String MDC_TRANSACTION_ID_KEY = "transactionId";

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String transactionId =
                request.getHeader(TRANSACTION_ID_HEADER);

        /*
         * Option 1:
         * Generate an ID when the client does not send one.
         *
         * This is usually more convenient than rejecting the request.
         */
        if (!StringUtils.hasText(transactionId)) {
            transactionId = UUID.randomUUID().toString();
        }

        try {
            MDC.put(MDC_TRANSACTION_ID_KEY, transactionId);

            response.setHeader(
                    TRANSACTION_ID_HEADER,
                    transactionId
            );

            log.info(
                    "Incoming request. method={}, path={}, remoteAddress={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getRemoteAddr()
            );

            long startTime = System.currentTimeMillis();

            filterChain.doFilter(request, response);

            long duration = System.currentTimeMillis() - startTime;

            log.info(
                    "Request completed. method={}, path={}, status={}, durationMs={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    duration
            );

        } catch (Exception exception) {

            log.error(
                    "Request processing failed. method={}, path={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    exception
            );

            throw exception;

        } finally {
            /*
             * Important because servlet threads are reused.
             */
            MDC.remove(MDC_TRANSACTION_ID_KEY);
        }
    }
}