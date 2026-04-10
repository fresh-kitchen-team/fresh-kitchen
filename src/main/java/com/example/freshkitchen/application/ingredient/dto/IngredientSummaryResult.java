package com.example.freshkitchen.application.ingredient.dto;

import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import com.example.freshkitchen.domain.ingredient.enums.IngredientStatus;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;

import java.time.LocalDate;

public record IngredientSummaryResult(
        Long ingredientId,
        String name,
        IngredientStatus status,
        Long storageId,
        String storageName,
        StorageType storageType,
        Long catalogId,
        LocalDate expiresAt
) {

    public static IngredientSummaryResult from(Ingredient ingredient) {
        return new IngredientSummaryResult(
                ingredient.getId(),
                ingredient.getName(),
                ingredient.getStatus(),
                ingredient.getStorage().getId(),
                ingredient.getStorage().getName(),
                ingredient.getStorage().getStorageType(),
                ingredient.getCatalog() != null ? ingredient.getCatalog().getId() : null,
                ingredient.getExpiresAt()
        );
    }
}
