package com.example.freshkitchen.presentation.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class AuthRequest {

    private AuthRequest() {
    }

    public record DevLogin(
            @NotNull @Positive Long userId
    ) {
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
