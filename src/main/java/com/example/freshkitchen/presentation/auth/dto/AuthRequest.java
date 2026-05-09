package com.example.freshkitchen.presentation.auth.dto;

import jakarta.validation.constraints.NotBlank;

public final class AuthRequest {

    private AuthRequest() {
    }

    public record GoogleLogin(
            @NotBlank String idToken
    ) {
    }

    public record KakaoLogin(
            @NotBlank String idToken
    ) {
    }
}
