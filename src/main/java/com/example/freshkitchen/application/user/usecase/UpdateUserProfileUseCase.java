package com.example.freshkitchen.application.user.usecase;

import com.example.freshkitchen.domain.user.enums.AllergyType;
import com.example.freshkitchen.domain.user.enums.CookingTool;
import com.example.freshkitchen.domain.user.enums.FoodStyle;

import java.util.Set;

public interface UpdateUserProfileUseCase {

    void update(Command command);

    record Command(
            Long userId,
            String nickname,
            String profileImageUrl,
            boolean profileImageUrlSet,
            String bio,
            boolean bioSet,
            Set<String> preferredIngredients,
            Set<FoodStyle> foodStyles,
            Set<AllergyType> allergies,
            Set<CookingTool> cookingTools
    ) {
    }
}
