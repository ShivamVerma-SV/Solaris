package com.solaris.backend.security;

import com.solaris.backend.exception.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;

@Component
public class SecurityErrorWriter {
    private final ObjectMapper objectMapper;

    public SecurityErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void writeUnauthorized(HttpServletRequest request, HttpServletResponse response, String message)
            throws IOException {
        write(request, response, HttpStatus.UNAUTHORIZED, message);
    }

    public void writeForbidden(HttpServletRequest request, HttpServletResponse response, String message)
            throws IOException {
        write(request, response, HttpStatus.FORBIDDEN, message);
    }

    public void writeServiceUnavailable(HttpServletRequest request, HttpServletResponse response, String message)
            throws IOException {
        write(request, response, HttpStatus.SERVICE_UNAVAILABLE, message);
    }

    private void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        objectMapper.writeValue(response.getOutputStream(), ApiErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .build());
    }
}
