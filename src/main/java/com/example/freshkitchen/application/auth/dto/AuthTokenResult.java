package com.example.freshkitchen.application.auth.dto;

public record AuthTokenResult(
        String accessToken,
        String refreshToken,
        boolean newUser
) {
}
