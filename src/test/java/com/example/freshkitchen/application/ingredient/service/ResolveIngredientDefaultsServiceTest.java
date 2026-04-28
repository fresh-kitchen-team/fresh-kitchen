package com.example.freshkitchen.application.ingredient.service;

import com.example.freshkitchen.application.ingredient.dto.IngredientDto;
import com.example.freshkitchen.application.ingredient.usecase.ResolveIngredientDefaultsUseCase;
import com.example.freshkitchen.domain.catalog.entity.CatalogExpiryRule;
import com.example.freshkitchen.domain.catalog.entity.CategoryExpiryRule;
import com.example.freshkitchen.domain.catalog.entity.IngredientCatalog;
import com.example.freshkitchen.domain.catalog.enums.CatalogCategory;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;
import com.example.freshkitchen.support.PostgreSqlTestContainerSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestConstructor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DataJpaTest
@Import(ResolveIngredientDefaultsService.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class ResolveIngredientDefaultsServiceTest extends PostgreSqlTestContainerSupport {

    private final ResolveIngredientDefaultsUseCase resolveIngredientDefaultsUseCase;

    @PersistenceContext
    private EntityManager entityManager;

    ResolveIngredientDefaultsServiceTest(ResolveIngredientDefaultsUseCase resolveIngredientDefaultsUseCase) {
        this.resolveIngredientDefaultsUseCase = resolveIngredientDefaultsUseCase;
    }

    @Test
    void resolve_prefersCatalogRule() {
        IngredientCatalog catalog = persistCatalog("Milk", CatalogCategory.DAIRY, StorageType.FRIDGE);
        persistCatalogExpiryRule(catalog, StorageType.FRIDGE, 7, "catalog-rule");
        persistCategoryExpiryRule(CatalogCategory.DAIRY, StorageType.FRIDGE, 3, "category-rule");

        IngredientDto.DefaultsResponse result = resolveIngredientDefaultsUseCase.resolve(
                new ResolveIngredientDefaultsUseCase.Query(catalog.getId(), CatalogCategory.DAIRY, StorageType.FRIDGE)
        );

        assertEquals(catalog.getId(), result.catalogId());
        assertEquals(StorageType.FRIDGE, result.defaultStorageType());
        assertEquals(7, result.shelfLifeDays());
        assertEquals("catalog-rule", result.referenceNote());
    }

    @Test
    void resolve_fallsBackToCategoryRule() {
        IngredientCatalog catalog = persistCatalog("Apple", CatalogCategory.FRUIT, StorageType.PANTRY);
        persistCategoryExpiryRule(CatalogCategory.FRUIT, StorageType.PANTRY, 5, "category-rule");

        IngredientDto.DefaultsResponse result = resolveIngredientDefaultsUseCase.resolve(
                new ResolveIngredientDefaultsUseCase.Query(catalog.getId(), CatalogCategory.FRUIT, StorageType.PANTRY)
        );

        assertEquals(catalog.getId(), result.catalogId());
        assertEquals(StorageType.PANTRY, result.defaultStorageType());
        assertEquals(5, result.shelfLifeDays());
        assertEquals("category-rule", result.referenceNote());
    }

    @Test
    void resolve_usesCatalogCategoryForCategoryRuleFallback() {
        IngredientCatalog catalog = persistCatalog("Apple", CatalogCategory.FRUIT, StorageType.PANTRY);
        persistCategoryExpiryRule(CatalogCategory.FRUIT, StorageType.PANTRY, 5, "catalog-category-rule");
        persistCategoryExpiryRule(CatalogCategory.VEGETABLE, StorageType.PANTRY, 2, "query-category-rule");

        IngredientDto.DefaultsResponse result = resolveIngredientDefaultsUseCase.resolve(
                new ResolveIngredientDefaultsUseCase.Query(catalog.getId(), CatalogCategory.VEGETABLE, StorageType.PANTRY)
        );

        assertEquals(catalog.getId(), result.catalogId());
        assertEquals(StorageType.PANTRY, result.defaultStorageType());
        assertEquals(5, result.shelfLifeDays());
        assertEquals("catalog-category-rule", result.referenceNote());
    }

    @Test
    void resolve_returnsEmptyDefaultsWhenNoRuleExists() {
        IngredientDto.DefaultsResponse result = resolveIngredientDefaultsUseCase.resolve(
                new ResolveIngredientDefaultsUseCase.Query(null, CatalogCategory.ETC, StorageType.PANTRY)
        );

        assertNull(result.catalogId());
        assertEquals(StorageType.PANTRY, result.defaultStorageType());
        assertNull(result.shelfLifeDays());
        assertNull(result.referenceNote());
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

    private void persistCatalogExpiryRule(
            IngredientCatalog catalog,
            StorageType storageType,
            int shelfLifeDays,
            String referenceNote
    ) {
        CatalogExpiryRule rule = CatalogExpiryRule.create(new CatalogExpiryRule.CreateCommand(
                catalog,
                storageType,
                shelfLifeDays,
                referenceNote
        ));
        entityManager.persist(rule);
    }

    private void persistCategoryExpiryRule(
            CatalogCategory category,
            StorageType storageType,
            int shelfLifeDays,
            String referenceNote
    ) {
        CategoryExpiryRule rule = CategoryExpiryRule.create(new CategoryExpiryRule.CreateCommand(
                category,
                storageType,
                shelfLifeDays,
                referenceNote
        ));
        entityManager.persist(rule);
    }
}
