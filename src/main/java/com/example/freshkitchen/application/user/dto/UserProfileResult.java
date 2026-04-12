package com.example.freshkitchen.application.user.dto;

import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.entity.UserProfile;
import com.example.freshkitchen.domain.user.enums.AllergyType;
import com.example.freshkitchen.domain.user.enums.CookingTool;
import com.example.freshkitchen.domain.user.enums.FoodStyle;

import java.util.Set;

public record UserProfileResult(
        Long userId,
        String nickname,
        String profileImageUrl,
        String bio,
        Set<String> preferredIngredients,
        Set<FoodStyle> foodStyles,
        Set<AllergyType> allergies,
        Set<CookingTool> cookingTools
) {

    public static UserProfileResult from(User user) {
        UserProfile profile = user.getProfile();
        if (profile == null) {  // 프로필 없는 user는 예외가 아니라 비어있는 결과로 표현
            return new UserProfileResult(
                    user.getId(),
                    null,
                    null,
                    null,
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of()
            );
        }

        return new UserProfileResult(
                user.getId(),
                profile.getNickname(),
                profile.getProfileImageUrl(),
                profile.getBio(),
                Set.copyOf(profile.getPreferredIngredients()),
                Set.copyOf(profile.getFoodStyles()),
                Set.copyOf(profile.getAllergies()),
                Set.copyOf(profile.getCookingTools())
        );
    }
}
