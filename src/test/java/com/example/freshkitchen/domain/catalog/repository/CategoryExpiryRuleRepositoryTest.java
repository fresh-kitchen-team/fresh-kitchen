package com.example.freshkitchen.domain.catalog.repository;

import com.example.freshkitchen.domain.catalog.entity.CategoryExpiryRule;
import com.example.freshkitchen.domain.catalog.enums.CatalogCategory;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;
import com.example.freshkitchen.support.PostgreSqlTestContainerSupport;
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
class CategoryExpiryRuleRepositoryTest extends PostgreSqlTestContainerSupport {

    private final CategoryExpiryRuleRepository categoryExpiryRuleRepository;

    CategoryExpiryRuleRepositoryTest(CategoryExpiryRuleRepository categoryExpiryRuleRepository) {
        this.categoryExpiryRuleRepository = categoryExpiryRuleRepository;
    }

    @Test
    void findByCategoryAndStorageType_returnsMatchingRule() {
        CategoryExpiryRule foundRule = categoryExpiryRuleRepository.findByCategoryAndStorageType(
                        CatalogCategory.VEGETABLE,
                        StorageType.FRIDGE
                )
                .orElseThrow();

        assertEquals(7, foundRule.getShelfLifeDays());
    }

    @Test
    void findAllByCategory_returnsRulesOrderedByStorageType() {
        List<StorageType> storageTypes = categoryExpiryRuleRepository.findAllByCategory(CatalogCategory.FRUIT).stream()
                .map(CategoryExpiryRule::getStorageType)
                .toList();

        assertIterableEquals(List.of(StorageType.FREEZER, StorageType.FRIDGE, StorageType.PANTRY), storageTypes);
    }

    @Test
    void findByCategoryAndStorageType_returnsEmptyWhenRuleDoesNotExist() {
        Optional<CategoryExpiryRule> rule = categoryExpiryRuleRepository.findByCategoryAndStorageType(
                null,
                StorageType.PANTRY
        );

        assertTrue(rule.isEmpty());
    }
}
