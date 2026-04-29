package com.example.freshkitchen.application.ingredient.service;

import com.example.freshkitchen.application.ingredient.usecase.CreateIngredientUseCase;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@Import({DefaultStorageService.class, CreateIngredientService.class})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class CreateIngredientServiceTest extends PostgreSqlTestContainerSupport {

    private final CreateIngredientUseCase createIngredientUseCase;
    private final IngredientRepository ingredientRepository;

    @PersistenceContext
    private EntityManager entityManager;

    CreateIngredientServiceTest(
            CreateIngredientUseCase createIngredientUseCase,
            IngredientRepository ingredientRepository
    ) {
        this.createIngredientUseCase = createIngredientUseCase;
        this.ingredientRepository = ingredientRepository;
    }

    @Test
    void create_persistsIngredientWithOwnedStorage() {
        User user = persistUser("create-user", Provider.GOOGLE);
        Storage storage = persistStorage(user, StorageType.FRIDGE, "Fridge");
        IngredientCatalog catalog = persistCatalog("Tomato", CatalogCategory.VEGETABLE, StorageType.FRIDGE);

        Long ingredientId = createIngredientUseCase.create(new CreateIngredientUseCase.Command(
                user.getId(),
                storage.getId(),
                catalog.getId(),
                "Tomato",
                null,
                null,
                ExpirySourceType.POLICY,
                "salad",
                IngredientSourceType.MANUAL
        ));

        entityManager.flush();
        entityManager.clear();

        Ingredient ingredient = ingredientRepository.findByIdAndUserId(ingredientId, user.getId())
                .orElseThrow();

        assertEquals("Tomato", ingredient.getName());
        assertEquals(storage.getId(), ingredient.getStorage().getId());
        assertEquals(catalog.getId(), ingredient.getCatalog().getId());
        assertEquals("salad", ingredient.getNote());
    }

    @Test
    void create_rejectsStorageNotOwnedByUser() {
        User owner = persistUser("owner-user", Provider.GOOGLE);
        User otherUser = persistUser("other-user", Provider.KAKAO);
        Storage storage = persistStorage(otherUser, StorageType.FRIDGE, "Other fridge");

        IngredientException exception = assertThrows(
                IngredientException.class,
                () -> createIngredientUseCase.create(new CreateIngredientUseCase.Command(
                        owner.getId(),
                        storage.getId(),
                        null,
                        "Onion",
                        null,
                        null,
                        ExpirySourceType.UNKNOWN,
                        null,
                        IngredientSourceType.MANUAL
                ))
        );

        assertEquals("storage not found", exception.getMessage());
    }

    @Test
    void create_rejectsMissingCatalog() {
        User user = persistUser("catalog-user", Provider.GOOGLE);
        Storage storage = persistStorage(user, StorageType.PANTRY, "Pantry");

        IngredientException exception = assertThrows(
                IngredientException.class,
                () -> createIngredientUseCase.create(new CreateIngredientUseCase.Command(
                        user.getId(),
                        storage.getId(),
                        Long.MAX_VALUE,
                        "Sauce",
                        null,
                        null,
                        ExpirySourceType.UNKNOWN,
                        null,
                        IngredientSourceType.MANUAL
                ))
        );

        assertEquals("ingredient catalog not found", exception.getMessage());
    }

    @Test
    void create_bootstrapsMissingDefaultStoragesBeforePersistingIngredient() {
        User user = persistUser("bootstrap-user", Provider.GOOGLE);
        Storage pantry = persistStorage(user, StorageType.PANTRY, "Pantry");

        Long ingredientId = createIngredientUseCase.create(new CreateIngredientUseCase.Command(
                user.getId(),
                pantry.getId(),
                null,
                "Soy sauce",
                null,
                null,
                ExpirySourceType.UNKNOWN,
                null,
                IngredientSourceType.MANUAL
        ));

        entityManager.flush();
        entityManager.clear();

        assertEquals(3, entityManager.createQuery("""
                select count(storage)
                from Storage storage
                where storage.user.id = :userId
                """, Long.class)
                .setParameter("userId", user.getId())
                .getSingleResult());
        assertEquals("Soy sauce", ingredientRepository.findByIdAndUserId(ingredientId, user.getId())
                .orElseThrow()
                .getName());
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
