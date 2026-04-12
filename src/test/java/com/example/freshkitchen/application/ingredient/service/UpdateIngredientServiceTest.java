package com.example.freshkitchen.application.ingredient.service;

import com.example.freshkitchen.application.ingredient.usecase.UpdateIngredientUseCase;
import com.example.freshkitchen.domain.catalog.entity.IngredientCatalog;
import com.example.freshkitchen.domain.catalog.enums.CatalogCategory;
import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import com.example.freshkitchen.domain.ingredient.entity.Storage;
import com.example.freshkitchen.domain.ingredient.exception.IngredientException;
import com.example.freshkitchen.domain.ingredient.enums.ExpirySourceType;
import com.example.freshkitchen.domain.ingredient.enums.IngredientSourceType;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;
import com.example.freshkitchen.domain.ingredient.repository.IngredientRepository;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.enums.Provider;
import com.example.freshkitchen.support.PostgreSqlTestContainerSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestConstructor;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@Import({DefaultStorageService.class, UpdateIngredientService.class})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class UpdateIngredientServiceTest extends PostgreSqlTestContainerSupport {

    private final UpdateIngredientUseCase updateIngredientUseCase;
    private final IngredientRepository ingredientRepository;

    @PersistenceContext
    private EntityManager entityManager;

    UpdateIngredientServiceTest(
            UpdateIngredientUseCase updateIngredientUseCase,
            IngredientRepository ingredientRepository
    ) {
        this.updateIngredientUseCase = updateIngredientUseCase;
        this.ingredientRepository = ingredientRepository;
    }

    @Test
    void update_appliesPartialChangesAndNullableClears() {
        User user = persistUser("update-user", Provider.GOOGLE);
        Storage storage = persistStorage(user, StorageType.FRIDGE, "Fridge");
        Storage nextStorage = persistStorage(user, StorageType.FREEZER, "Freezer");
        IngredientCatalog catalog = persistCatalog("Milk", CatalogCategory.DAIRY, StorageType.FRIDGE);
        Ingredient ingredient = Ingredient.create(new Ingredient.CreateCommand(
                user,
                storage,
                catalog,
                "Milk",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 5),
                ExpirySourceType.MANUAL,
                "memo",
                IngredientSourceType.MANUAL
        ));
        entityManager.persist(ingredient);

        updateIngredientUseCase.update(new UpdateIngredientUseCase.Command(
                ingredient.getId(),
                user.getId(),
                nextStorage.getId(),
                null,
                true,
                "Skim milk",
                null,
                true,
                LocalDate.of(2026, 4, 10),
                true,
                ExpirySourceType.POLICY,
                null,
                true,
                IngredientSourceType.RECEIPT
        ));

        entityManager.flush();
        entityManager.clear();

        Ingredient updatedIngredient = ingredientRepository.findByIdAndUserId(ingredient.getId(), user.getId())
                .orElseThrow();

        assertEquals("Skim milk", updatedIngredient.getName());
        assertEquals(nextStorage.getId(), updatedIngredient.getStorage().getId());
        assertNull(updatedIngredient.getCatalog());
        assertNull(updatedIngredient.getRegisteredAt());
        assertEquals(LocalDate.of(2026, 4, 10), updatedIngredient.getExpiresAt());
        assertEquals(ExpirySourceType.POLICY, updatedIngredient.getExpirySourceType());
        assertNull(updatedIngredient.getNote());
        assertEquals(IngredientSourceType.RECEIPT, updatedIngredient.getSourceType());
    }

    @Test
    void update_rejectsStorageOfAnotherUser() {
        User owner = persistUser("owner-user", Provider.GOOGLE);
        User otherUser = persistUser("other-user", Provider.KAKAO);
        Storage ownerStorage = persistStorage(owner, StorageType.FRIDGE, "Owner fridge");
        Storage otherStorage = persistStorage(otherUser, StorageType.FRIDGE, "Other fridge");
        Ingredient ingredient = Ingredient.create(new Ingredient.CreateCommand(
                owner,
                ownerStorage,
                null,
                "Apple",
                null,
                null,
                ExpirySourceType.UNKNOWN,
                null,
                IngredientSourceType.MANUAL
        ));
        entityManager.persist(ingredient);

        IngredientException exception = assertThrows(
                IngredientException.class,
                () -> updateIngredientUseCase.update(new UpdateIngredientUseCase.Command(
                        ingredient.getId(),
                        owner.getId(),
                        otherStorage.getId(),
                        null,
                        false,
                        null,
                        null,
                        false,
                        null,
                        false,
                        null,
                        null,
                        false,
                        null
                ))
        );

        assertEquals("storage not found", exception.getMessage());
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

    private IngredientCatalog persistCatalog(String name, CatalogCategory category, StorageType defaultStorageType) {
        IngredientCatalog catalog = IngredientCatalog.create(new IngredientCatalog.CreateCommand(
                null,
                name,
                category,
                defaultStorageType,
                null
        ));
        entityManager.persist(catalog);
        return catalog;
    }
}
