package com.example.freshkitchen.domain.catalog.repository;

import com.example.freshkitchen.domain.catalog.entity.CatalogExpiryRule;
import com.example.freshkitchen.domain.catalog.entity.IngredientCatalog;
import com.example.freshkitchen.domain.catalog.enums.CatalogCategory;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;
import com.example.freshkitchen.support.PostgreSqlTestContainerSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestConstructor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class IngredientCatalogRepositoryTest extends PostgreSqlTestContainerSupport {

    private final IngredientCatalogRepository ingredientCatalogRepository;

    @PersistenceContext
    private EntityManager entityManager;

    IngredientCatalogRepositoryTest(IngredientCatalogRepository ingredientCatalogRepository) {
        this.ingredientCatalogRepository = ingredientCatalogRepository;
    }

    @Test
    void findByName_returnsMatchingCatalog() {
        IngredientCatalog catalog = persistCatalog("Tomato", CatalogCategory.VEGETABLE, StorageType.FRIDGE);

        entityManager.flush();
        entityManager.clear();

        IngredientCatalog foundCatalog = ingredientCatalogRepository.findByName("Tomato")
                .orElseThrow();

        assertEquals(catalog.getId(), foundCatalog.getId());
        assertEquals(CatalogCategory.VEGETABLE, foundCatalog.getCategory());
    }

    @Test
    void findAllByCategory_returnsMatchingCatalogsOrderedByName() {
        IngredientCatalog banana = persistCatalog("Banana", CatalogCategory.FRUIT, StorageType.PANTRY);
        persistCatalog("Carrot", CatalogCategory.VEGETABLE, StorageType.FRIDGE);
        IngredientCatalog apple = persistCatalog("Apple", CatalogCategory.FRUIT, StorageType.FRIDGE);

        entityManager.flush();
        entityManager.clear();

        List<Long> catalogIds = ingredientCatalogRepository.findAllByCategory(CatalogCategory.FRUIT).stream()
                .map(IngredientCatalog::getId)
                .toList();

        assertIterableEquals(List.of(apple.getId(), banana.getId()), catalogIds);
    }

    @Test
    void findByIdWithExpiryRules_fetchesCatalogExpiryRules() {
        IngredientCatalog catalog = persistCatalog("Milk", CatalogCategory.DAIRY, StorageType.FRIDGE);
        CatalogExpiryRule fridgeRule = persistCatalogExpiryRule(catalog, StorageType.FRIDGE, 7);
        CatalogExpiryRule freezerRule = persistCatalogExpiryRule(catalog, StorageType.FREEZER, 30);

        entityManager.flush();
        entityManager.clear();

        IngredientCatalog foundCatalog = ingredientCatalogRepository.findByIdWithExpiryRules(catalog.getId())
                .orElseThrow();

        assertTrue(Hibernate.isInitialized(foundCatalog.getCatalogExpiryRules()));
        assertEquals(2, foundCatalog.getCatalogExpiryRules().size());
        assertTrue(foundCatalog.getCatalogExpiryRules().stream()
                .anyMatch(rule -> rule.getId().equals(fridgeRule.getId())));
        assertTrue(foundCatalog.getCatalogExpiryRules().stream()
                .anyMatch(rule -> rule.getId().equals(freezerRule.getId())));
    }

    @Test
    void findByName_returnsEmptyWhenCatalogDoesNotExist() {
        Optional<IngredientCatalog> catalog = ingredientCatalogRepository.findByName("missing");

        assertTrue(catalog.isEmpty());
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

    private CatalogExpiryRule persistCatalogExpiryRule(
            IngredientCatalog catalog,
            StorageType storageType,
            int shelfLifeDays
    ) {
        CatalogExpiryRule catalogExpiryRule = CatalogExpiryRule.create(new CatalogExpiryRule.CreateCommand(
                catalog,
                storageType,
                shelfLifeDays,
                null
        ));
        entityManager.persist(catalogExpiryRule);
        return catalogExpiryRule;
    }
}
