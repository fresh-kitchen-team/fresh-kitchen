package com.example.freshkitchen.application.ingredient.dto.response;

import com.example.freshkitchen.domain.ingredient.enums.StorageType;

public record IngredientDefaultsResponse(
        Long catalogId,
        StorageType defaultStorageType,
        Integer shelfLifeDays,
        String referenceNote
) {
}
