package com.example.freshkitchen.global.exception.response;

import com.example.freshkitchen.global.exception.ErrorCode;

import java.time.OffsetDateTime;

public record ErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String code,
        String message,
        String path
) {

    public static ErrorResponse of(ErrorCode errorCode, String message, String path) {
        return new ErrorResponse(
                OffsetDateTime.now(),
                errorCode.status().value(),
                errorCode.code(),
                message,
                path
        );
    }
}
