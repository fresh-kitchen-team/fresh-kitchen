package com.example.freshkitchen.application.ingredient.service;

import com.example.freshkitchen.application.ingredient.usecase.CreateIngredientUseCase;
import com.example.freshkitchen.domain.catalog.entity.CatalogExpiryRule;
import com.example.freshkitchen.domain.catalog.entity.CategoryExpiryRule;
import com.example.freshkitchen.domain.catalog.entity.IngredientCatalogAlias;
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
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestConstructor;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@Import({
        DefaultStorageService.class,
        IngredientCatalogMappingService.class,
        ResolveIngredientDefaultsService.class,
        CreateIngredientService.class,
        CreateIngredientServiceTest.FixedClockConfig.class
})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class CreateIngredientServiceTest extends PostgreSqlTestContainerSupport {

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 1);

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
                StorageType.FRIDGE,
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
        assertEquals(CatalogCategory.VEGETABLE, ingredient.getCategory());
        assertEquals("salad", ingredient.getNote());
    }

    @Test
    void create_mapsCatalogByIngredientNameAndAppliesCatalogExpiryRuleAsPolicy() {
        User user = persistUser("catalog-mapping-user", Provider.GOOGLE);
        Storage storage = persistStorage(user, StorageType.FRIDGE, "Fridge");
        IngredientCatalog catalog = persistCatalog("Tomato", CatalogCategory.VEGETABLE, StorageType.FRIDGE);
        persistCatalogExpiryRule(catalog, StorageType.FRIDGE, 7);

        Long ingredientId = createIngredientUseCase.create(new CreateIngredientUseCase.Command(
                user.getId(),
                StorageType.FRIDGE,
                null,
                "Tomato",
                LocalDate.of(2026, 5, 1),
                null,
                ExpirySourceType.MANUAL,
                "salad",
                IngredientSourceType.PHOTO
        ));

        entityManager.flush();
        entityManager.clear();

        Ingredient ingredient = ingredientRepository.findByIdAndUserId(ingredientId, user.getId())
                .orElseThrow();

        assertEquals(storage.getId(), ingredient.getStorage().getId());
        assertEquals(catalog.getId(), ingredient.getCatalog().getId());
        assertEquals(CatalogCategory.VEGETABLE, ingredient.getCategory());
        assertEquals(LocalDate.of(2026, 5, 8), ingredient.getExpiresAt());
        assertEquals(ExpirySourceType.POLICY, ingredient.getExpirySourceType());
    }

    @Test
    void create_mapsCatalogByAliasNameAndAppliesCatalogExpiryRuleAsPolicy() {
        User user = persistUser("alias-mapping-user", Provider.GOOGLE);
        Storage storage = persistStorage(user, StorageType.FRIDGE, "Fridge");
        IngredientCatalog catalog = persistCatalog("베이컨", CatalogCategory.MEAT, StorageType.FRIDGE);
        persistCatalogAlias(catalog, "슬라이스베이컨", "KO");
        persistCatalogExpiryRule(catalog, StorageType.FRIDGE, 7);

        Long ingredientId = createIngredientUseCase.create(new CreateIngredientUseCase.Command(
                user.getId(),
                StorageType.FRIDGE,
                null,
                "슬라이스베이컨",
                LocalDate.of(2026, 5, 1),
                null,
                ExpirySourceType.MANUAL,
                "sandwich",
                IngredientSourceType.PHOTO
        ));

        entityManager.flush();
        entityManager.clear();

        Ingredient ingredient = ingredientRepository.findByIdAndUserId(ingredientId, user.getId())
                .orElseThrow();

        assertEquals("슬라이스베이컨", ingredient.getName());
        assertEquals(storage.getId(), ingredient.getStorage().getId());
        assertEquals(catalog.getId(), ingredient.getCatalog().getId());
        assertEquals(CatalogCategory.MEAT, ingredient.getCategory());
        assertEquals(LocalDate.of(2026, 5, 8), ingredient.getExpiresAt());
        assertEquals(ExpirySourceType.POLICY, ingredient.getExpirySourceType());
    }

    @Test
    void create_doesNotMapCatalogByPartialSubstringWithoutAlias() {
        User user = persistUser("partial-substring-user", Provider.GOOGLE);
        persistStorage(user, StorageType.FRIDGE, "Fridge");
        persistCatalog("베이컨", CatalogCategory.MEAT, StorageType.FRIDGE);

        Long ingredientId = createIngredientUseCase.create(new CreateIngredientUseCase.Command(
                user.getId(),
                StorageType.FRIDGE,
                null,
                "슬라이스베이컨",
                LocalDate.of(2026, 5, 1),
                null,
                ExpirySourceType.MANUAL,
                null,
                IngredientSourceType.PHOTO
        ));

        entityManager.flush();
        entityManager.clear();

        Ingredient ingredient = ingredientRepository.findByIdAndUserId(ingredientId, user.getId())
                .orElseThrow();

        assertNull(ingredient.getCatalog());
        assertNull(ingredient.getCategory());
        assertNull(ingredient.getExpiresAt());
        assertEquals(ExpirySourceType.UNKNOWN, ingredient.getExpirySourceType());
    }

    @Test
    void create_appliesCategoryExpiryRuleAsPolicyWhenCatalogRuleDoesNotExist() {
        User user = persistUser("category-policy-user", Provider.GOOGLE);
        persistStorage(user, StorageType.FRIDGE, "Fridge");
        IngredientCatalog catalog = persistCatalog("Zucchini", CatalogCategory.VEGETABLE, StorageType.FRIDGE);

        Long ingredientId = createIngredientUseCase.create(new CreateIngredientUseCase.Command(
                user.getId(),
                StorageType.FRIDGE,
                catalog.getId(),
                "Zucchini",
                LocalDate.of(2026, 5, 1),
                null,
                ExpirySourceType.MANUAL,
                null,
                IngredientSourceType.MANUAL
        ));

        entityManager.flush();
        entityManager.clear();

        Ingredient ingredient = ingredientRepository.findByIdAndUserId(ingredientId, user.getId())
                .orElseThrow();

        assertEquals(LocalDate.of(2026, 5, 8), ingredient.getExpiresAt());
        assertEquals(ExpirySourceType.POLICY, ingredient.getExpirySourceType());
    }

    @Test
    void create_usesClockDateWhenApplyingPolicyRuleWithoutRegisteredAt() {
        User user = persistUser("clock-policy-user", Provider.GOOGLE);
        persistStorage(user, StorageType.FRIDGE, "Fridge");
        IngredientCatalog catalog = persistCatalog("Clock tomato", CatalogCategory.VEGETABLE, StorageType.FRIDGE);
        persistCatalogExpiryRule(catalog, StorageType.FRIDGE, 3);

        Long ingredientId = createIngredientUseCase.create(new CreateIngredientUseCase.Command(
                user.getId(),
                StorageType.FRIDGE,
                catalog.getId(),
                "Clock tomato",
                null,
                null,
                ExpirySourceType.MANUAL,
                null,
                IngredientSourceType.MANUAL
        ));

        entityManager.flush();
        entityManager.clear();

        Ingredient ingredient = ingredientRepository.findByIdAndUserId(ingredientId, user.getId())
                .orElseThrow();

        assertEquals(TODAY.plusDays(3), ingredient.getExpiresAt());
        assertEquals(ExpirySourceType.POLICY, ingredient.getExpirySourceType());
    }

    @Test
    void create_doesNotMapManualIngredientByName() {
        User user = persistUser("manual-no-mapping-user", Provider.GOOGLE);
        persistStorage(user, StorageType.FRIDGE, "Fridge");
        persistCatalog("Tomato", CatalogCategory.VEGETABLE, StorageType.FRIDGE);

        Long ingredientId = createIngredientUseCase.create(new CreateIngredientUseCase.Command(
                user.getId(),
                StorageType.FRIDGE,
                null,
                "Tomato",
                LocalDate.of(2026, 5, 1),
                null,
                ExpirySourceType.UNKNOWN,
                null,
                IngredientSourceType.MANUAL
        ));

        entityManager.flush();
        entityManager.clear();

        Ingredient ingredient = ingredientRepository.findByIdAndUserId(ingredientId, user.getId())
                .orElseThrow();

        assertNull(ingredient.getCatalog());
        assertNull(ingredient.getCategory());
        assertNull(ingredient.getExpiresAt());
        assertEquals(ExpirySourceType.UNKNOWN, ingredient.getExpirySourceType());
    }

    @Test
    void create_leavesCategoryNullForUnmappedScanCatalogWithoutAutoExpiry() {
        User user = persistUser("unmapped-catalog-user", Provider.GOOGLE);
        persistStorage(user, StorageType.FRIDGE, "Fridge");

        Long ingredientId = createIngredientUseCase.create(new CreateIngredientUseCase.Command(
                user.getId(),
                StorageType.FRIDGE,
                null,
                "Unknown food",
                LocalDate.of(2026, 5, 1),
                null,
                ExpirySourceType.MANUAL,
                null,
                IngredientSourceType.RECEIPT
        ));

        entityManager.flush();
        entityManager.clear();

        Ingredient ingredient = ingredientRepository.findByIdAndUserId(ingredientId, user.getId())
                .orElseThrow();

        assertEquals("Unknown food", ingredient.getName());
        assertNull(ingredient.getCatalog());
        assertNull(ingredient.getCategory());
        assertNull(ingredient.getExpiresAt());
        assertEquals(ExpirySourceType.UNKNOWN, ingredient.getExpirySourceType());
    }

    @Test
    void create_leavesCategoryNullWhenCatalogAndRequestedCategoryAreMissing() {
        User user = persistUser("default-category-user", Provider.GOOGLE);
        persistStorage(user, StorageType.FRIDGE, "Fridge");

        Long ingredientId = createIngredientUseCase.create(new CreateIngredientUseCase.Command(
                user.getId(),
                StorageType.FRIDGE,
                null,
                "Unknown food",
                LocalDate.of(2026, 5, 1),
                null,
                ExpirySourceType.MANUAL,
                null,
                IngredientSourceType.MANUAL
        ));

        entityManager.flush();
        entityManager.clear();

        Ingredient ingredient = ingredientRepository.findByIdAndUserId(ingredientId, user.getId())
                .orElseThrow();

        assertNull(ingredient.getCatalog());
        assertNull(ingredient.getCategory());
        assertNull(ingredient.getExpiresAt());
        assertEquals(ExpirySourceType.UNKNOWN, ingredient.getExpirySourceType());
    }

    @Test
    void create_usesRequestedExpiryDateBeforePolicyRule() {
        User user = persistUser("manual-expiry-user", Provider.GOOGLE);
        persistStorage(user, StorageType.FRIDGE, "Fridge");
        IngredientCatalog catalog = persistCatalog("Tomato", CatalogCategory.VEGETABLE, StorageType.FRIDGE);
        persistCatalogExpiryRule(catalog, StorageType.FRIDGE, 7);

        Long ingredientId = createIngredientUseCase.create(new CreateIngredientUseCase.Command(
                user.getId(),
                StorageType.FRIDGE,
                null,
                "Tomato",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 20),
                ExpirySourceType.MANUAL,
                null,
                IngredientSourceType.PHOTO
        ));

        entityManager.flush();
        entityManager.clear();

        Ingredient ingredient = ingredientRepository.findByIdAndUserId(ingredientId, user.getId())
                .orElseThrow();

        assertEquals(LocalDate.of(2026, 5, 20), ingredient.getExpiresAt());
        assertEquals(ExpirySourceType.MANUAL, ingredient.getExpirySourceType());
    }

    @Test
    void create_mapsStorageTypeToAuthenticatedUsersStorage() {
        User owner = persistUser("owner-user", Provider.GOOGLE);
        User otherUser = persistUser("other-user", Provider.KAKAO);
        Storage ownerStorage = persistStorage(owner, StorageType.FRIDGE, "Owner fridge");
        persistStorage(otherUser, StorageType.FRIDGE, "Other fridge");

        Long ingredientId = createIngredientUseCase.create(new CreateIngredientUseCase.Command(
                owner.getId(),
                StorageType.FRIDGE,
                null,
                "Onion",
                null,
                null,
                ExpirySourceType.UNKNOWN,
                null,
                IngredientSourceType.MANUAL
        ));

        entityManager.flush();
        entityManager.clear();

        Ingredient ingredient = ingredientRepository.findByIdAndUserId(ingredientId, owner.getId())
                .orElseThrow();
        assertEquals(ownerStorage.getId(), ingredient.getStorage().getId());
    }

    @Test
    void create_rejectsMissingCatalog() {
        User user = persistUser("catalog-user", Provider.GOOGLE);
        persistStorage(user, StorageType.PANTRY, "Pantry");

        IngredientException exception = assertThrows(
                IngredientException.class,
                () -> createIngredientUseCase.create(new CreateIngredientUseCase.Command(
                        user.getId(),
                        StorageType.PANTRY,
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
        persistStorage(user, StorageType.PANTRY, "Pantry");

        Long ingredientId = createIngredientUseCase.create(new CreateIngredientUseCase.Command(
                user.getId(),
                StorageType.FREEZER,
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
        Ingredient ingredient = ingredientRepository.findByIdAndUserId(ingredientId, user.getId())
                .orElseThrow();
        assertEquals("Soy sauce", ingredient.getName());
        assertEquals(StorageType.FREEZER, ingredient.getStorage().getStorageType());
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

    private void persistCatalogExpiryRule(IngredientCatalog catalog, StorageType storageType, int shelfLifeDays) {
        entityManager.persist(CatalogExpiryRule.create(
                new CatalogExpiryRule.CreateCommand(
                        catalog,
                        storageType,
                        shelfLifeDays,
                        "test rule"
                )
        ));
    }

    private void persistCatalogAlias(IngredientCatalog catalog, String aliasName, String language) {
        entityManager.persist(IngredientCatalogAlias.create(new IngredientCatalogAlias.CreateCommand(
                catalog,
                aliasName,
                IngredientCatalogMappingService.normalizeAliasName(aliasName),
                language
        )));
    }

    private void persistCategoryExpiryRule(CatalogCategory category, StorageType storageType, int shelfLifeDays) {
        entityManager.persist(CategoryExpiryRule.create(
                new CategoryExpiryRule.CreateCommand(
                        category,
                        storageType,
                        shelfLifeDays,
                        "test category rule"
                )
        ));
    }

    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        Clock clock() {
            return Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        }
    }
}
