package com.example.freshkitchen.global.security.exception;

import com.example.freshkitchen.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum OAuthErrorCode implements ErrorCode {

    INVALID_ID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-401-7", "invalid id token"),
    PROVIDER_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "AUTH-400-1", "oauth provider not supported"),
    OAUTH_PROVIDER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AUTH-503-1", "oauth provider unavailable"),
    USER_RESOLUTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH-500-1", "failed to resolve user during oauth login");

    private final HttpStatus status;
    private final String code;
    private final String message;

    OAuthErrorCode(HttpStatus status, String code, String message) {
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
