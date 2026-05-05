package com.example.freshkitchen.global.security.infrastructure;

import com.example.freshkitchen.global.security.exception.JwtErrorCode;
import com.example.freshkitchen.global.security.exception.JwtTokenException;
import com.example.freshkitchen.global.security.exception.SecurityErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationEntryPointTest {

    private JwtAuthenticationEntryPoint entryPoint;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        entryPoint = new JwtAuthenticationEntryPoint(objectMapper);
        request = new MockHttpServletRequest("GET", "/api/v1/users/1/profile");
        response = new MockHttpServletResponse();
    }

    @Test
    void commence_returnsAuthenticationRequired_whenNoJwtException() throws Exception {
        entryPoint.commence(request, response,
                new AuthenticationCredentialsNotFoundException("no credentials"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        assertThat(response.getCharacterEncoding()).isEqualTo(StandardCharsets.UTF_8.name());

        String body = response.getContentAsString();
        assertThat(body).contains("\"code\":\"" + SecurityErrorCode.AUTHENTICATION_REQUIRED.code() + "\"");
        assertThat(body).contains("\"message\":\"" + SecurityErrorCode.AUTHENTICATION_REQUIRED.message() + "\"");
        assertThat(body).contains("\"path\":\"/api/v1/users/1/profile\"");
        assertThat(body).contains("\"status\":401");
    }

    @Test
    void commence_returnsSpecificJwtError_whenJwtExceptionIsPresent() throws Exception {
        request.setAttribute(JwtAuthenticationFilter.JWT_EXCEPTION_ATTRIBUTE,
                new JwtTokenException(JwtErrorCode.EXPIRED_TOKEN));

        entryPoint.commence(request, response,
                new AuthenticationCredentialsNotFoundException("no credentials"));

        assertThat(response.getStatus()).isEqualTo(401);
        String body = response.getContentAsString();
        assertThat(body).contains("\"code\":\"" + JwtErrorCode.EXPIRED_TOKEN.code() + "\"");
        assertThat(body).contains("\"message\":\"" + JwtErrorCode.EXPIRED_TOKEN.message() + "\"");
    }

    @Test
    void commence_returnsInvalidSignatureError_whenSignatureExceptionIsPresent() throws Exception {
        request.setAttribute(JwtAuthenticationFilter.JWT_EXCEPTION_ATTRIBUTE,
                new JwtTokenException(JwtErrorCode.INVALID_SIGNATURE));

        entryPoint.commence(request, response,
                new AuthenticationCredentialsNotFoundException("no credentials"));

        String body = response.getContentAsString();
        assertThat(body).contains("\"code\":\"" + JwtErrorCode.INVALID_SIGNATURE.code() + "\"");
    }

    @Test
    void commence_includesTimestampField() throws Exception {
        entryPoint.commence(request, response,
                new AuthenticationCredentialsNotFoundException("no credentials"));

        String body = response.getContentAsString();
        assertThat(body).contains("\"timestamp\"");
    }
}
