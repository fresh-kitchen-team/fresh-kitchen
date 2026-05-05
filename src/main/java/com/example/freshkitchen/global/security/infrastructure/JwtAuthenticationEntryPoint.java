package com.example.freshkitchen.global.security.infrastructure;

import com.example.freshkitchen.global.exception.ErrorCode;
import com.example.freshkitchen.global.exception.response.ErrorResponse;
import com.example.freshkitchen.global.security.exception.JwtTokenException;
import com.example.freshkitchen.global.security.exception.SecurityErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        Object attribute = request.getAttribute(JwtAuthenticationFilter.JWT_EXCEPTION_ATTRIBUTE);

        ErrorCode errorCode = (attribute instanceof JwtTokenException jwtException)
                ? jwtException.getErrorCode()
                : SecurityErrorCode.AUTHENTICATION_REQUIRED;

        ErrorResponse errorResponse = ErrorResponse.of(
                errorCode, errorCode.message(), request.getRequestURI()
        );

        response.setStatus(errorCode.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
