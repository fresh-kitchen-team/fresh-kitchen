package com.example.freshkitchen.presentation.chat.dto.request;

public record AiSettingSaveRequest(
        boolean responseStyle,
        boolean priorityExpiration,
        boolean priorityNutrition,
        boolean priorityFrequent,
        boolean provideExtraInfo
) {}
