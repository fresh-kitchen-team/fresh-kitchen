package com.example.freshkitchen.global.security.exception;

import com.example.freshkitchen.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum JwtErrorCode implements ErrorCode {

    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-401-1", "expired token"),
    INVALID_SIGNATURE(HttpStatus.UNAUTHORIZED, "AUTH-401-2", "invalid token signature"),
    MALFORMED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-401-3", "malformed token"),
    UNSUPPORTED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-401-4", "unsupported token"),
    EMPTY_CLAIMS(HttpStatus.UNAUTHORIZED, "AUTH-401-5", "token claims are empty"),
    NOT_YET_VALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-401-6", "token is not yet valid"),
    BLACKLISTED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-401-9", "token has been invalidated by logout"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-401-8", "invalid or expired refresh token");

    private final HttpStatus status;
    private final String code;
    private final String message;

    JwtErrorCode(HttpStatus status, String code, String message) {
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
