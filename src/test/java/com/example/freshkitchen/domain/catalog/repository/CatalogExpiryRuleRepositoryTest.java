package com.example.freshkitchen.domain.catalog.repository;

import com.example.freshkitchen.domain.catalog.entity.CatalogExpiryRule;
import com.example.freshkitchen.domain.catalog.entity.IngredientCatalog;
import com.example.freshkitchen.domain.catalog.enums.CatalogCategory;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;
import com.example.freshkitchen.support.PostgreSqlTestContainerSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestConstructor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class CatalogExpiryRuleRepositoryTest extends PostgreSqlTestContainerSupport {

    private final CatalogExpiryRuleRepository catalogExpiryRuleRepository;

    @PersistenceContext
    private EntityManager entityManager;

    CatalogExpiryRuleRepositoryTest(CatalogExpiryRuleRepository catalogExpiryRuleRepository) {
        this.catalogExpiryRuleRepository = catalogExpiryRuleRepository;
    }

    @Test
    void findByCatalogIdAndStorageType_returnsMatchingRule() {
        IngredientCatalog catalog = persistCatalog("Yogurt", CatalogCategory.DAIRY, StorageType.FRIDGE);
        CatalogExpiryRule rule = persistCatalogExpiryRule(catalog, StorageType.FRIDGE, 10);

        entityManager.flush();
        entityManager.clear();

        CatalogExpiryRule foundRule = catalogExpiryRuleRepository.findByCatalogIdAndStorageType(
                        catalog.getId(),
                        StorageType.FRIDGE
                )
                .orElseThrow();

        assertEquals(rule.getId(), foundRule.getId());
        assertEquals(10, foundRule.getShelfLifeDays());
    }

    @Test
    void findAllByCatalogId_returnsRulesOrderedByStorageType() {
        IngredientCatalog catalog = persistCatalog("Beef", CatalogCategory.MEAT, StorageType.FRIDGE);
        CatalogExpiryRule freezerRule = persistCatalogExpiryRule(catalog, StorageType.FREEZER, 60);
        CatalogExpiryRule fridgeRule = persistCatalogExpiryRule(catalog, StorageType.FRIDGE, 5);
        IngredientCatalog otherCatalog = persistCatalog("Chicken", CatalogCategory.MEAT, StorageType.FRIDGE);
        persistCatalogExpiryRule(otherCatalog, StorageType.FRIDGE, 4);

        entityManager.flush();
        entityManager.clear();

        List<Long> ruleIds = catalogExpiryRuleRepository.findAllByCatalogId(catalog.getId()).stream()
                .map(CatalogExpiryRule::getId)
                .toList();

        assertIterableEquals(List.of(freezerRule.getId(), fridgeRule.getId()), ruleIds);
    }

    @Test
    void findByCatalogIdAndStorageType_returnsEmptyWhenRuleDoesNotExist() {
        Optional<CatalogExpiryRule> rule = catalogExpiryRuleRepository.findByCatalogIdAndStorageType(
                Long.MAX_VALUE,
                StorageType.PANTRY
        );

        assertTrue(rule.isEmpty());
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
