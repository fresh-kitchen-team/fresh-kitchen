package com.example.freshkitchen.presentation.chat.dto.response;

import com.example.freshkitchen.domain.chat.entity.AiSetting;

public record AiSettingResponse(
        boolean responseStyle,
        boolean priorityExpiration,
        boolean priorityNutrition,
        boolean priorityFrequent,
        boolean provideExtraInfo
) {
    public static AiSettingResponse from(AiSetting setting) {
        return new AiSettingResponse(
                "간단".equals(setting.getResponseStyle()),
                setting.isPriorityExpiration(),
                setting.isPriorityNutrition(),
                setting.isPriorityFrequent(),
                setting.isProvideExtraInfo()
        );
    }
}