package com.example.freshkitchen.application.user.service;

import com.example.freshkitchen.application.user.usecase.DeleteUserProfileUseCase;
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
@Import(DeleteUserProfileService.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class DeleteUserProfileServiceTest extends PostgreSqlTestContainerSupport {

    private final DeleteUserProfileUseCase deleteUserProfileUseCase;
    private final UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    DeleteUserProfileServiceTest(
            DeleteUserProfileUseCase deleteUserProfileUseCase,
            UserRepository userRepository
    ) {
        this.deleteUserProfileUseCase = deleteUserProfileUseCase;
        this.userRepository = userRepository;
    }

    @Test // delete는 user를 지우는 것이 아니라 UserProfile 연결만 제거
    void delete_removesProfileWhenExists() {
        User user = persistUser("profile-user-5", Provider.GOOGLE);
        persistProfile(user);
        flushAndClear();

        deleteUserProfileUseCase.delete(new DeleteUserProfileUseCase.Command(user.getId()));

        entityManager.flush();
        entityManager.clear();

        User foundUser = userRepository.findByIdWithProfile(user.getId())
                .orElseThrow();

        assertNull(foundUser.getProfile());
    }

    @Test
    void delete_isNoOpWhenProfileDoesNotExist() {
        User user = persistUser("profile-user-6", Provider.KAKAO);
        flushAndClear();

        deleteUserProfileUseCase.delete(new DeleteUserProfileUseCase.Command(user.getId()));

        entityManager.flush();
        entityManager.clear();

        User foundUser = userRepository.findByIdWithProfile(user.getId())
                .orElseThrow();

        assertNull(foundUser.getProfile());
    }

    @Test
    void delete_rejectsMissingUser() {
        UserException exception = assertThrows(
                UserException.class,
                () -> deleteUserProfileUseCase.delete(new DeleteUserProfileUseCase.Command(Long.MAX_VALUE))
        );

        assertEquals("user not found", exception.getMessage());
    }

    @Test
    void delete_rejectsNullUserId() {
        BusinessValidationException exception = assertThrows(
                BusinessValidationException.class,
                () -> deleteUserProfileUseCase.delete(new DeleteUserProfileUseCase.Command(null))
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

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
