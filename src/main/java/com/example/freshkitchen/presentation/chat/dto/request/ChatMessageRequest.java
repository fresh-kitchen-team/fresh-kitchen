package com.example.freshkitchen.presentation.chat.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChatMessageRequest(
        @NotBlank String message,
        AiSettingRequest aiSetting
) {
    public record AiSettingRequest(
            boolean responseStyle,
            boolean priorityExpiration,
            boolean priorityNutrition,
            boolean priorityFrequent,
            boolean provideExtraInfo
    ) {}
}
