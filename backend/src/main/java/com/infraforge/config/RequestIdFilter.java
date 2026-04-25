package com.infraforge.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Assigns a unique request ID to every incoming HTTP request.
 *
 * The ID is:
 *   1. Read from the X-Request-ID header if the client provides one
 *   2. Generated as a UUID if no header is present
 *
 * The ID is stored in the SLF4J MDC so it appears in every log line
 * for the duration of that request. It is also echoed back in the
 * X-Request-ID response header so clients can correlate their request
 * with server logs.
 *
 * Log pattern references this as: %X{requestId}
 */
@Component
public class RequestIdFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Always clear MDC to avoid leaking into thread pool reuse
            MDC.remove(MDC_KEY);
        }
    }
}