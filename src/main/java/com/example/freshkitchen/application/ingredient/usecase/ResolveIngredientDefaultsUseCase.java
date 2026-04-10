package com.example.freshkitchen.application.ingredient.usecase;

import com.example.freshkitchen.application.ingredient.dto.IngredientDefaultsResult;
import com.example.freshkitchen.domain.catalog.enums.CatalogCategory;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;

public interface ResolveIngredientDefaultsUseCase {

    IngredientDefaultsResult resolve(Query query);

    record Query(
            Long catalogId,
            CatalogCategory category,
            StorageType storageType
    ) {
    }
}
