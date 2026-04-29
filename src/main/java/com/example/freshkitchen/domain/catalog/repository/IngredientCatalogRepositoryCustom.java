package com.example.freshkitchen.domain.catalog.repository;

import com.example.freshkitchen.domain.catalog.entity.IngredientCatalog;
import com.example.freshkitchen.domain.catalog.enums.CatalogCategory;

import java.util.List;
import java.util.Optional;

public interface IngredientCatalogRepositoryCustom {

    Optional<IngredientCatalog> findByName(String name);

    Optional<IngredientCatalog> findByIdWithExpiryRules(Long catalogId);

    List<IngredientCatalog> findAllByCategory(CatalogCategory category);
}
