package com.example.freshkitchen.infrastructure.ai.exception;

import com.example.freshkitchen.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum AiServerErrorCode implements ErrorCode {

    AI_SERVER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AI-503-1", "AI server unavailable"),
    AI_SERVER_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "AI-504-1", "AI server timeout"),
    AI_RESPONSE_INVALID(HttpStatus.BAD_GATEWAY, "AI-502-1", "AI response invalid");

    private final HttpStatus status;
    private final String code;
    private final String message;

    AiServerErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
