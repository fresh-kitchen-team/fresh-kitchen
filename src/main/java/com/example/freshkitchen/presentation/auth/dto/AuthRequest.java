package com.example.freshkitchen.presentation.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthRequest {

    private AuthRequest() {
    }

    public record GoogleLogin(
            @NotBlank @Size(max = 4096) String idToken
    ) {
    }

    public record KakaoLogin(
            @NotBlank @Size(max = 4096) String idToken
    ) {
    }

    public record RefreshToken(
            @NotBlank @Size(max = 4096) String refreshToken
    ) {
    }
}
