package com.example.freshkitchen.domain.user.entity;

import com.example.freshkitchen.domain.user.enums.AllergyType;
import com.example.freshkitchen.domain.user.enums.CookingTool;
import com.example.freshkitchen.domain.user.enums.FoodStyle;
import com.example.freshkitchen.domain.user.enums.Provider;
import com.example.freshkitchen.domain.user.enums.UserStatus;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserEntityTest {

    @Test
    void createAndApply_manageUserStateWithinEntity() {
        User user = User.create(new User.CreateCommand("provider-1", Provider.GOOGLE));

        // status를 INACTIVE로 바꿀 때, inactiveAt은 현재 시각으로 자동 설정
        user.apply(new User.UpdateCommand("provider-2", UserStatus.INACTIVE, null));

        assertEquals("provider-2", user.getProviderUserId());
        assertEquals(UserStatus.INACTIVE, user.getStatus());
        assertNotNull(user.getInactiveAt());
    }

    @Test
    void userProfileApply_replacesElementCollections() {
        User user = User.create(new User.CreateCommand("provider-1", Provider.KAKAO));
        UserProfile profile = UserProfile.create(new UserProfile.CreateCommand(
                user,
                "fresh",
                "https://image.example/profile.png",
                "bio",
                Set.of("onion"),
                Set.of(FoodStyle.KOREAN),
                Set.of(AllergyType.EGG),
                Set.of(CookingTool.PAN)
        ));

        // profile 수정 시 컬렉션 필드는 전달된 값으로 통째로 교체
        profile.apply(new UserProfile.UpdateCommand(
                "chef",
                null,
                false,
                null,
                false,
                Set.of("garlic", "carrot"),
                Set.of(FoodStyle.WESTERN, FoodStyle.JAPANESE),
                Set.of(AllergyType.MILK),
                Set.of(CookingTool.OVEN, CookingTool.BLENDER)
        ));
        user.apply(new User.UpdateCommand(null, UserStatus.ACTIVE, null));

        assertEquals(user, profile.getUser());
        assertEquals(profile, user.getProfile());
        assertEquals("chef", profile.getNickname());
        assertEquals(Set.of("garlic", "carrot"), profile.getPreferredIngredients());
        assertEquals(Set.of(FoodStyle.WESTERN, FoodStyle.JAPANESE), profile.getFoodStyles());
        assertEquals(Set.of(AllergyType.MILK), profile.getAllergies());
        assertEquals(Set.of(CookingTool.OVEN, CookingTool.BLENDER), profile.getCookingTools());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertNull(user.getInactiveAt());
    }

    @Test   // (profileImageUrlSet / bioSet) 플래그로 유지할지, null로 비울지 구분하기
    void userProfileApply_canClearOptionalFields() {
        User user = User.create(new User.CreateCommand("provider-1", Provider.KAKAO));
        UserProfile profile = UserProfile.create(new UserProfile.CreateCommand(
                user,
                "fresh",
                "https://image.example/profile.png",
                "bio",
                Set.of("onion"),
                Set.of(FoodStyle.KOREAN),
                Set.of(AllergyType.EGG),
                Set.of(CookingTool.PAN)
        ));

        profile.apply(new UserProfile.UpdateCommand(
                null,
                null,
                true,
                null,
                true,
                null,
                null,
                null,
                null
        ));

        assertNull(profile.getProfileImageUrl());
        assertNull(profile.getBio());
    }

    @Test
    void userProfileCreate_requiresUser() {
        BusinessValidationException exception = assertThrows(BusinessValidationException.class, () ->
                UserProfile.create(new UserProfile.CreateCommand( // profile은 반드시 User aggregate에 연결된 상태로만 생성 가능
                        null,
                        "fresh",
                        null,
                        null,
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        Set.of()
                ))
        );

        assertEquals("user must not be null", exception.getMessage());
    }
}
