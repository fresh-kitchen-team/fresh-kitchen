package com.example.freshkitchen.domain.chat.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ChatMessageRequest(
        @NotBlank String message,
        @NotBlank String type,
        @Valid @NotEmpty List<IngredientRequest> ingredients,
        @Valid UserPreferencesRequest userPreferences
) {
    public record IngredientRequest(
            Long id,
            String name,
            Double quantity,
            String unit,
            String expiresAt
    ) {}

    public record UserPreferencesRequest(
            List<String> allergies,
            List<String> preferredIngredients,
            List<String> preferredFoodStyles,
            List<String> cookingTool
    ) {}
}