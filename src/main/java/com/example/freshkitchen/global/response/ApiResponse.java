package com.example.freshkitchen.global.response;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Objects;

public record ApiResponse<T>(
        int status,
        String code,
        String message,
        T data
) {

    private static final String SUCCESS_CODE_PREFIX = "COMMON-";
    private static final String SUCCESS_MESSAGE = "Success";

    public static <T> ApiResponse<T> onSuccess(T data) {
        return onSuccess(HttpStatus.OK, data);
    }

    public static <T> ApiResponse<T> onSuccess(HttpStatus status, T data) {
        validateSuccessStatus(status);
        return new ApiResponse<>(
                status.value(),
                SUCCESS_CODE_PREFIX + status.value(),
                SUCCESS_MESSAGE,
                data
        );
    }

    public static <T> ResponseEntity<ApiResponse<T>> success(T data) {
        return success(HttpStatus.OK, data);
    }

    public static <T> ResponseEntity<ApiResponse<T>> success(HttpStatus status, T data) {
        return ResponseEntity
                .status(status)
                .body(onSuccess(status, data));
    }

    private static void validateSuccessStatus(HttpStatus status) {
        Objects.requireNonNull(status, "status must not be null");
        if (!status.is2xxSuccessful()) {
            throw new IllegalStateException("success status must be 2xx");
        }
    }
}
