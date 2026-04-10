package com.example.freshkitchen.application.ingredient.dto;

import com.example.freshkitchen.domain.ingredient.enums.StorageType;

public record IngredientDefaultsResult(
        Long catalogId,
        StorageType defaultStorageType,
        Integer shelfLifeDays,
        String referenceNote
) {
}
