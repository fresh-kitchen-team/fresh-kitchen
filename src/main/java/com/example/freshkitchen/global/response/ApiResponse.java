package com.example.freshkitchen.global.response;

import org.springframework.http.HttpStatus;

public record ApiResponse<T>(
        int status,
        String code,
        String message,
        T data
) {

    private static final String SUCCESS_CODE = "COMMON-200";
    private static final String SUCCESS_MESSAGE = "Success";

    public static <T> ApiResponse<T> onSuccess(T data) {
        return new ApiResponse<>(
                HttpStatus.OK.value(),
                SUCCESS_CODE,
                SUCCESS_MESSAGE,
                data
        );
    }
}
