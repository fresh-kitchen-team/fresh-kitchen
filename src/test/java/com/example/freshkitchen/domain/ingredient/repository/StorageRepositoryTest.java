package com.example.freshkitchen.domain.ingredient.repository;

import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import com.example.freshkitchen.domain.ingredient.entity.Storage;
import com.example.freshkitchen.domain.ingredient.enums.ExpirySourceType;
import com.example.freshkitchen.domain.ingredient.enums.IngredientSourceType;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.enums.Provider;
import com.example.freshkitchen.support.PostgreSqlTestContainerSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestConstructor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class StorageRepositoryTest extends PostgreSqlTestContainerSupport {

    private final StorageRepository storageRepository;

    @PersistenceContext
    private EntityManager entityManager;

    StorageRepositoryTest(StorageRepository storageRepository) {
        this.storageRepository = storageRepository;
    }

    @Test
    void findByIdAndUserId_returnsOnlyOwnedStorage() {
        User owner = persistUser("owner-user", Provider.GOOGLE);
        User otherUser = persistUser("other-user", Provider.KAKAO);
        Storage storage = persistStorage(owner, StorageType.FRIDGE, "Main fridge");

        entityManager.flush();
        entityManager.clear();

        Optional<Storage> foundStorage = storageRepository.findByIdAndUserId(storage.getId(), owner.getId());
        Optional<Storage> notFoundStorage = storageRepository.findByIdAndUserId(storage.getId(), otherUser.getId());

        assertTrue(foundStorage.isPresent());
        assertEquals(storage.getId(), foundStorage.orElseThrow().getId());
        assertTrue(notFoundStorage.isEmpty());
    }

    @Test
    void findByIdAndUserIdWithIngredients_fetchesConnectedIngredients() {
        User user = persistUser("storage-user", Provider.GOOGLE);
        Storage storage = persistStorage(user, StorageType.FRIDGE, "Fridge");
        Ingredient firstIngredient = persistIngredient(user, storage, "Tomato");
        Ingredient secondIngredient = persistIngredient(user, storage, "Milk");

        entityManager.flush();
        entityManager.clear();

        Storage foundStorage = storageRepository.findByIdAndUserIdWithIngredients(storage.getId(), user.getId())
                .orElseThrow();

        assertTrue(Hibernate.isInitialized(foundStorage.getIngredients()));
        assertEquals(2, foundStorage.getIngredients().size());
        assertTrue(foundStorage.getIngredients().stream()
                .anyMatch(ingredient -> ingredient.getId().equals(firstIngredient.getId())));
        assertTrue(foundStorage.getIngredients().stream()
                .anyMatch(ingredient -> ingredient.getId().equals(secondIngredient.getId())));
    }

    @Test
    void findAllByUserId_returnsOnlyUserStorages() {
        User user = persistUser("list-user", Provider.GOOGLE);
        User otherUser = persistUser("other-list-user", Provider.KAKAO);
        Storage firstStorage = persistStorage(user, StorageType.FRIDGE, "Fridge");
        Storage secondStorage = persistStorage(user, StorageType.FREEZER, "Freezer");
        persistStorage(otherUser, StorageType.PANTRY, "Pantry");

        entityManager.flush();
        entityManager.clear();

        List<Long> storageIds = storageRepository.findAllByUserId(user.getId()).stream()
                .map(Storage::getId)
                .toList();

        assertIterableEquals(List.of(firstStorage.getId(), secondStorage.getId()), storageIds);
    }

    @Test
    void findByIdAndUserIdWithIngredients_returnsEmptyWhenStorageDoesNotExist() {
        assertFalse(storageRepository.findByIdAndUserIdWithIngredients(Long.MAX_VALUE, 1L).isPresent());
    }

    private User persistUser(String providerUserId, Provider provider) {
        User user = User.create(new User.CreateCommand(providerUserId, provider));
        entityManager.persist(user);
        return user;
    }

    private Storage persistStorage(User user, StorageType storageType, String name) {
        Storage storage = Storage.create(new Storage.CreateCommand(user, storageType, name));
        entityManager.persist(storage);
        return storage;
    }

    private Ingredient persistIngredient(User user, Storage storage, String name) {
        Ingredient ingredient = Ingredient.create(new Ingredient.CreateCommand(
                user,
                storage,
                null,
                name,
                null,
                null,
                ExpirySourceType.UNKNOWN,
                null,
                IngredientSourceType.MANUAL
        ));
        entityManager.persist(ingredient);
        return ingredient;
    }
}
