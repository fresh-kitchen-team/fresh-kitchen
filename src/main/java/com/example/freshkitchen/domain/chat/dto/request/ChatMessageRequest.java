package com.example.freshkitchen.domain.chat.dto.request;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageRequest {

    @NotBlank
    private String message;

    @NotBlank
    private String type;

    @Valid
    @NotEmpty
    private List<IngredientRequest> ingredients;

    @Valid
    private UserPreferencesRequest userPreferences;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IngredientRequest {
        private Long id;
        private String name;
        private Double quantity;
        private String unit;
        private String expiresAt;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserPreferencesRequest {
        private List<String> allergies;
        private List<String> preferredIngredients;
        private List<String> preferredFoodStyles;
        private List<String> cookingTool;
    }
}