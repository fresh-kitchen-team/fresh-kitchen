package com.example.freshkitchen.domain.user.repository;

import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.entity.UserProfile;
import com.example.freshkitchen.domain.user.enums.AllergyType;
import com.example.freshkitchen.domain.user.enums.CookingTool;
import com.example.freshkitchen.domain.user.enums.FoodStyle;
import com.example.freshkitchen.domain.user.enums.Provider;
import com.example.freshkitchen.support.PostgreSqlTestContainerSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestConstructor;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class UserRepositoryTest extends PostgreSqlTestContainerSupport {

    private final UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    UserRepositoryTest(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Test
    void findByProviderAndProviderUserId_returnsMatchingUser() {
        User user = persistUserWithProfile("provider-user-1", Provider.GOOGLE);

        entityManager.flush();
        entityManager.clear();

        User foundUser = userRepository.findByProviderAndProviderUserId(Provider.GOOGLE, "provider-user-1")
                .orElseThrow();

        assertEquals(user.getId(), foundUser.getId());
        assertEquals(Provider.GOOGLE, foundUser.getProvider());
        assertEquals("provider-user-1", foundUser.getProviderUserId());
    }

    @Test
    void existsByProviderAndProviderUserId_returnsTrueOnlyForExistingUser() {
        persistUserWithProfile("provider-user-2", Provider.KAKAO);

        entityManager.flush();
        entityManager.clear();

        assertTrue(userRepository.existsByProviderAndProviderUserId(Provider.KAKAO, "provider-user-2"));
        assertFalse(userRepository.existsByProviderAndProviderUserId(Provider.KAKAO, "missing-user"));
    }

    @Test
    void findByIdWithProfile_loadsUserAggregate() {
        User user = persistUserWithProfile("provider-user-3", Provider.GOOGLE);

        entityManager.flush();
        entityManager.clear();

        User foundUser = userRepository.findByIdWithProfile(user.getId())
                .orElseThrow();

        assertTrue(Hibernate.isInitialized(foundUser.getProfile()));

        UserProfile profile = foundUser.getProfile();
        assertNotNull(profile);
        assertTrue(Hibernate.isInitialized(profile.getPreferredIngredients()));
        assertTrue(Hibernate.isInitialized(profile.getFoodStyles()));
        assertTrue(Hibernate.isInitialized(profile.getAllergies()));
        assertTrue(Hibernate.isInitialized(profile.getCookingTools()));
        assertEquals("fresh-user", profile.getNickname());
        assertEquals(Set.of("garlic", "carrot"), profile.getPreferredIngredients());
        assertEquals(Set.of(FoodStyle.KOREAN, FoodStyle.WESTERN), profile.getFoodStyles());
        assertEquals(Set.of(AllergyType.EGG, AllergyType.MILK), profile.getAllergies());
        assertEquals(Set.of(CookingTool.OVEN, CookingTool.PAN), profile.getCookingTools());
    }

    @Test
    void findByIdWithProfile_returnsEmptyWhenUserDoesNotExist() {
        assertTrue(userRepository.findByIdWithProfile(Long.MAX_VALUE).isEmpty());
    }

    @Test
    void findByIdWithProfile_returnsUserEvenWhenProfileDoesNotExist() {
        User user = User.create(new User.CreateCommand("provider-user-4", Provider.KAKAO));
        entityManager.persist(user);

        entityManager.flush();
        entityManager.clear();

        User foundUser = userRepository.findByIdWithProfile(user.getId())
                .orElseThrow();

        assertEquals(user.getId(), foundUser.getId());
        assertNull(foundUser.getProfile());
    }

    private User persistUserWithProfile(String providerUserId, Provider provider) {
        User user = User.create(new User.CreateCommand(providerUserId, provider));
        entityManager.persist(user);

        UserProfile profile = UserProfile.create(new UserProfile.CreateCommand(
                user,
                "fresh-user",
                "https://cdn.example/profile.png",
                "hello",
                Set.of("garlic", "carrot"),
                Set.of(FoodStyle.KOREAN, FoodStyle.WESTERN),
                Set.of(AllergyType.EGG, AllergyType.MILK),
                Set.of(CookingTool.OVEN, CookingTool.PAN)
        ));
        entityManager.persist(profile);

        return user;
    }
}
