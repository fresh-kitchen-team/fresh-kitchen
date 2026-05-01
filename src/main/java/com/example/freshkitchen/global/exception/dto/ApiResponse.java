package com.example.freshkitchen.global.exception.dto;


import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record ApiResponse<T>(String message , T data,
                             @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
                             LocalDateTime timestamp) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("success", data, LocalDateTime.now());
    }


    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(message, null, LocalDateTime.now());
    }
    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(message, null, LocalDateTime.now());
    }

}