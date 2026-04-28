package com.example.freshkitchen.application.ingredient.dto.response;

import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import com.example.freshkitchen.domain.ingredient.enums.IngredientStatus;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;

import java.time.LocalDate;

public record IngredientSummaryResponse(
        Long ingredientId,
        String name,
        IngredientStatus status,
        Long storageId,
        String storageName,
        StorageType storageType,
        Long catalogId,
        LocalDate expiresAt
) {

    public static IngredientSummaryResponse from(Ingredient ingredient) {
        return new IngredientSummaryResponse(
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
