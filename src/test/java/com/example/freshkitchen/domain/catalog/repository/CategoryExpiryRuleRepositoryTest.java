package com.example.freshkitchen.domain.catalog.repository;

import com.example.freshkitchen.domain.catalog.entity.CategoryExpiryRule;
import com.example.freshkitchen.domain.catalog.enums.CatalogCategory;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;
import com.example.freshkitchen.support.PostgreSqlTestContainerSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
class CategoryExpiryRuleRepositoryTest extends PostgreSqlTestContainerSupport {

    private final CategoryExpiryRuleRepository categoryExpiryRuleRepository;

    @PersistenceContext
    private EntityManager entityManager;

    CategoryExpiryRuleRepositoryTest(CategoryExpiryRuleRepository categoryExpiryRuleRepository) {
        this.categoryExpiryRuleRepository = categoryExpiryRuleRepository;
    }

    @Test
    void findByCategoryAndStorageType_returnsMatchingRule() {
        CategoryExpiryRule rule = persistCategoryExpiryRule(CatalogCategory.VEGETABLE, StorageType.FRIDGE, 5);

        entityManager.flush();
        entityManager.clear();

        CategoryExpiryRule foundRule = categoryExpiryRuleRepository.findByCategoryAndStorageType(
                        CatalogCategory.VEGETABLE,
                        StorageType.FRIDGE
                )
                .orElseThrow();

        assertEquals(rule.getId(), foundRule.getId());
        assertEquals(5, foundRule.getShelfLifeDays());
    }

    @Test
    void findAllByCategory_returnsRulesOrderedByStorageType() {
        CategoryExpiryRule pantryRule = persistCategoryExpiryRule(CatalogCategory.FRUIT, StorageType.PANTRY, 3);
        CategoryExpiryRule fridgeRule = persistCategoryExpiryRule(CatalogCategory.FRUIT, StorageType.FRIDGE, 7);
        persistCategoryExpiryRule(CatalogCategory.MEAT, StorageType.FREEZER, 90);

        entityManager.flush();
        entityManager.clear();

        List<Long> ruleIds = categoryExpiryRuleRepository.findAllByCategory(CatalogCategory.FRUIT).stream()
                .map(CategoryExpiryRule::getId)
                .toList();

        assertIterableEquals(List.of(fridgeRule.getId(), pantryRule.getId()), ruleIds);
    }

    @Test
    void findByCategoryAndStorageType_returnsEmptyWhenRuleDoesNotExist() {
        Optional<CategoryExpiryRule> rule = categoryExpiryRuleRepository.findByCategoryAndStorageType(
                CatalogCategory.SEAFOOD,
                StorageType.PANTRY
        );

        assertTrue(rule.isEmpty());
    }

    private CategoryExpiryRule persistCategoryExpiryRule(
            CatalogCategory category,
            StorageType storageType,
            int shelfLifeDays
    ) {
        CategoryExpiryRule categoryExpiryRule = CategoryExpiryRule.create(new CategoryExpiryRule.CreateCommand(
                category,
                storageType,
                shelfLifeDays,
                null
        ));
        entityManager.persist(categoryExpiryRule);
        return categoryExpiryRule;
    }
}
