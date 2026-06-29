package com.example.freshkitchen.presentation.user.dto;

import com.example.freshkitchen.application.user.usecase.RegisterFcmTokenUseCase;
import com.example.freshkitchen.domain.user.enums.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class FcmTokenRequest {

    private FcmTokenRequest() {
    }

    public record Register(
            @NotBlank String tokenValue,
            @NotNull DeviceType deviceType
    ) {

        public RegisterFcmTokenUseCase.Command toCommand(Long userId) {
            return new RegisterFcmTokenUseCase.Command(userId, tokenValue, deviceType);
        }
    }
}