package com.example.freshkitchen.presentation.user.dto;

import com.example.freshkitchen.application.user.dto.UserProfileResult;
import com.example.freshkitchen.domain.user.enums.AllergyType;
import com.example.freshkitchen.domain.user.enums.CookingTool;
import com.example.freshkitchen.domain.user.enums.FoodStyle;

import java.util.Set;

public record UserProfileResponse(
        Long userId,
        String nickname,
        String profileImageUrl,
        String bio,
        Set<String> preferredIngredients,
        Set<FoodStyle> foodStyles,
        Set<AllergyType> allergies,
        Set<CookingTool> cookingTools
) {

    public static UserProfileResponse from(UserProfileResult result) {
        return new UserProfileResponse(
                result.userId(),
                result.nickname(),
                result.profileImageUrl(),
                result.bio(),
                Set.copyOf(result.preferredIngredients()),
                Set.copyOf(result.foodStyles()),
                Set.copyOf(result.allergies()),
                Set.copyOf(result.cookingTools())
        );
    }
}
