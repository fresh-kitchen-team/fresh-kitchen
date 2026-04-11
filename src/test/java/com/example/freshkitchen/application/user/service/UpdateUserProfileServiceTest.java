package com.example.freshkitchen.application.user.service;

import com.example.freshkitchen.application.user.usecase.UpdateUserProfileUseCase;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.entity.UserProfile;
import com.example.freshkitchen.domain.user.enums.AllergyType;
import com.example.freshkitchen.domain.user.enums.CookingTool;
import com.example.freshkitchen.domain.user.enums.FoodStyle;
import com.example.freshkitchen.domain.user.enums.Provider;
import com.example.freshkitchen.domain.user.exception.UserException;
import com.example.freshkitchen.domain.user.repository.UserRepository;
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
@Import(UpdateUserProfileService.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class UpdateUserProfileServiceTest extends PostgreSqlTestContainerSupport {

    private final UpdateUserProfileUseCase updateUserProfileUseCase;
    private final UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    UpdateUserProfileServiceTest(
            UpdateUserProfileUseCase updateUserProfileUseCase,
            UserRepository userRepository
    ) {
        this.updateUserProfileUseCase = updateUserProfileUseCase;
        this.userRepository = userRepository;
    }

    @Test
    void update_createsProfileWhenMissing() {
        User user = persistUser("profile-user-3", Provider.GOOGLE);
        flushAndClear();

        updateUserProfileUseCase.update(new UpdateUserProfileUseCase.Command(
                user.getId(),
                "fresh-user",
                "https://cdn.example/profile.png",
                true,
                "hello",
                true,
                Set.of("garlic", "carrot"),
                Set.of(FoodStyle.KOREAN, FoodStyle.WESTERN),
                Set.of(AllergyType.EGG, AllergyType.MILK),
                Set.of(CookingTool.OVEN, CookingTool.PAN)
        ));

        entityManager.flush();
        entityManager.clear();

        UserProfile profile = userRepository.findByIdWithProfile(user.getId())
                .orElseThrow()
                .getProfile();

        assertEquals("fresh-user", profile.getNickname());
        assertEquals("https://cdn.example/profile.png", profile.getProfileImageUrl());
        assertEquals("hello", profile.getBio());
        assertEquals(Set.of("garlic", "carrot"), profile.getPreferredIngredients());
        assertEquals(Set.of(FoodStyle.KOREAN, FoodStyle.WESTERN), profile.getFoodStyles());
        assertEquals(Set.of(AllergyType.EGG, AllergyType.MILK), profile.getAllergies());
        assertEquals(Set.of(CookingTool.OVEN, CookingTool.PAN), profile.getCookingTools());
    }

    @Test // null 컬렉션은 유지하고, 빈 Set은 명시적으로 비우는 계약을 검증
    void update_appliesPartialChangesAndKeepsUnsetCollections() {
        User user = persistUser("profile-user-4", Provider.KAKAO);
        persistProfile(user);
        flushAndClear();

        updateUserProfileUseCase.update(new UpdateUserProfileUseCase.Command(
                user.getId(),
                "chef-user",
                null,
                true,
                null,
                true,
                Set.of(),
                Set.of(FoodStyle.JAPANESE),
                null,
                Set.of(CookingTool.BLENDER)
        ));

        entityManager.flush();
        entityManager.clear();

        UserProfile profile = userRepository.findByIdWithProfile(user.getId())
                .orElseThrow()
                .getProfile();

        assertEquals("chef-user", profile.getNickname());
        assertNull(profile.getProfileImageUrl());
        assertNull(profile.getBio());
        assertEquals(Set.of(), profile.getPreferredIngredients());
        assertEquals(Set.of(FoodStyle.JAPANESE), profile.getFoodStyles());
        assertEquals(Set.of(AllergyType.EGG, AllergyType.MILK), profile.getAllergies());
        assertEquals(Set.of(CookingTool.BLENDER), profile.getCookingTools());
    }

    @Test
    void update_rejectsMissingNicknameWhenCreatingProfile() {
        User user = persistUser("profile-user-7", Provider.GOOGLE);
        flushAndClear();

        // profile 최초 생성 시에는 nickname이 필수라서, 누락되면 validation 예외를 던지도록
        BusinessValidationException exception = assertThrows(
                BusinessValidationException.class,
                () -> updateUserProfileUseCase.update(new UpdateUserProfileUseCase.Command(
                        user.getId(),
                        null,
                        null,
                        false,
                        null,
                        false,
                        null,
                        null,
                        null,
                        null
                ))
        );

        assertEquals("nickname must not be blank", exception.getMessage());
    }

    @Test
    void update_rejectsNullUserId() {
        BusinessValidationException exception = assertThrows(
                BusinessValidationException.class,
                () -> updateUserProfileUseCase.update(new UpdateUserProfileUseCase.Command(
                        null,
                        "fresh-user",
                        null,
                        false,
                        null,
                        false,
                        null,
                        null,
                        null,
                        null
                ))
        );

        assertEquals("userId must not be null", exception.getMessage());
    }

    @Test
    void update_rejectsMissingUser() {
        UserException exception = assertThrows(
                UserException.class,
                () -> updateUserProfileUseCase.update(new UpdateUserProfileUseCase.Command(
                        Long.MAX_VALUE,
                        "fresh-user",
                        null,
                        false,
                        null,
                        false,
                        null,
                        null,
                        null,
                        null
                ))
        );

        assertEquals("user not found", exception.getMessage());
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

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
