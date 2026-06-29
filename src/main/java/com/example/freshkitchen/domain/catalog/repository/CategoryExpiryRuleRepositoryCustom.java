package com.example.freshkitchen.domain.catalog.repository;

import com.example.freshkitchen.domain.catalog.entity.CategoryExpiryRule;
import com.example.freshkitchen.domain.catalog.enums.CatalogCategory;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;

import java.util.List;
import java.util.Optional;

public interface CategoryExpiryRuleRepositoryCustom {

    Optional<CategoryExpiryRule> findByCategoryAndStorageType(CatalogCategory category, StorageType storageType);

    List<CategoryExpiryRule> findAllByCategory(CatalogCategory category);
}
