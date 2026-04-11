package com.example.freshkitchen.application.user.service;

import com.example.freshkitchen.application.user.dto.UserProfileResult;
import com.example.freshkitchen.application.user.usecase.GetUserProfileUseCase;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.entity.UserProfile;
import com.example.freshkitchen.domain.user.enums.AllergyType;
import com.example.freshkitchen.domain.user.enums.CookingTool;
import com.example.freshkitchen.domain.user.enums.FoodStyle;
import com.example.freshkitchen.domain.user.enums.Provider;
import com.example.freshkitchen.domain.user.exception.UserException;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import com.example.freshkitchen.support.PostgreSqlTestContainerSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestConstructor;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@Import(GetUserProfileService.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class GetUserProfileServiceTest extends PostgreSqlTestContainerSupport {

    private final GetUserProfileUseCase getUserProfileUseCase;

    @PersistenceContext
    private EntityManager entityManager;

    GetUserProfileServiceTest(GetUserProfileUseCase getUserProfileUseCase) {
        this.getUserProfileUseCase = getUserProfileUseCase;
    }

    @Test
    void get_returnsProfileResultWhenProfileExists() {
        User user = persistUser("profile-user-1", Provider.GOOGLE);
        persistProfile(user);

        entityManager.flush();
        entityManager.clear();

        UserProfileResult result = getUserProfileUseCase.get(new GetUserProfileUseCase.Query(user.getId()));

        assertEquals(user.getId(), result.userId());
        assertEquals("fresh-user", result.nickname());
        assertEquals("https://cdn.example/profile.png", result.profileImageUrl());
        assertEquals("hello", result.bio());
        assertEquals(Set.of("garlic", "carrot"), result.preferredIngredients());
        assertEquals(Set.of(FoodStyle.KOREAN, FoodStyle.WESTERN), result.foodStyles());
        assertEquals(Set.of(AllergyType.EGG, AllergyType.MILK), result.allergies());
        assertEquals(Set.of(CookingTool.OVEN, CookingTool.PAN), result.cookingTools());
    }

    @Test
    void get_returnsEmptyProfileResultWhenProfileDoesNotExist() {
        User user = persistUser("profile-user-2", Provider.KAKAO);

        entityManager.flush();
        entityManager.clear();

        // 프로필 미작성 상태는 예외가 아니라, 빈 profile 결과로 반환
        UserProfileResult result = getUserProfileUseCase.get(new GetUserProfileUseCase.Query(user.getId()));

        assertEquals(user.getId(), result.userId());
        assertNull(result.nickname());
        assertNull(result.profileImageUrl());
        assertNull(result.bio());
        assertEquals(Set.of(), result.preferredIngredients());
        assertEquals(Set.of(), result.foodStyles());
        assertEquals(Set.of(), result.allergies());
        assertEquals(Set.of(), result.cookingTools());
    }

    @Test
    void get_rejectsMissingUser() {
        UserException exception = assertThrows(
                UserException.class,
                () -> getUserProfileUseCase.get(new GetUserProfileUseCase.Query(Long.MAX_VALUE))
        );

        // 조회기준은 profile 존재 여부가 아니라, user 존재 여부
        assertEquals("user not found", exception.getMessage());
    }

    @Test
    void get_rejectsNullUserId() {
        BusinessValidationException exception = assertThrows(
                BusinessValidationException.class,
                () -> getUserProfileUseCase.get(new GetUserProfileUseCase.Query(null))
        );

        assertEquals("userId must not be null", exception.getMessage());
    }

    private User persistUser(String providerUserId, Provider provider) {
        User user = User.create(new User.CreateCommand(providerUserId, provider));
        entityManager.persist(user);
        return user;
    }

    private void persistProfile(User user) {
        entityManager.persist(UserProfile.create(new UserProfile.CreateCommand(
                user,
                "fresh-user",
                "https://cdn.example/profile.png",
                "hello",
                Set.of("garlic", "carrot"),
                Set.of(FoodStyle.KOREAN, FoodStyle.WESTERN),
                Set.of(AllergyType.EGG, AllergyType.MILK),
                Set.of(CookingTool.OVEN, CookingTool.PAN)
        )));
    }
}
