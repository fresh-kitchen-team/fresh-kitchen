package com.example.freshkitchen.presentation.auth.dto;

import com.example.freshkitchen.application.auth.dto.AuthTokenResult;

public final class AuthResponse {

    private AuthResponse() {
    }

    public record Token(
            String accessToken,
            String refreshToken,
            boolean newUser
    ) {
        public static Token from(AuthTokenResult result) {
            return new Token(result.accessToken(), result.refreshToken(), result.newUser());
        }
    }
}
