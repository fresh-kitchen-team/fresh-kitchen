package com.example.freshkitchen.application.ingredient.dto;

import com.example.freshkitchen.domain.catalog.enums.CatalogCategory;
import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import com.example.freshkitchen.domain.ingredient.enums.ExpirySourceType;
import com.example.freshkitchen.domain.ingredient.enums.IngredientSourceType;
import com.example.freshkitchen.domain.ingredient.enums.IngredientStatus;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;

import java.time.LocalDate;

public record IngredientDetailResult(
        Long ingredientId,
        Long userId,
        Long storageId,
        String storageName,
        StorageType storageType,
        Long catalogId,
        String catalogName,
        CatalogCategory catalogCategory,
        String name,
        LocalDate registeredAt,
        LocalDate expiresAt,
        ExpirySourceType expirySourceType,
        IngredientStatus status,
        LocalDate consumedAt,
        LocalDate discardedAt,
        String note,
        IngredientSourceType sourceType
) {

    public static IngredientDetailResult from(Ingredient ingredient) {
        return new IngredientDetailResult(
                ingredient.getId(),
                ingredient.getUser().getId(),
                ingredient.getStorage().getId(),
                ingredient.getStorage().getName(),
                ingredient.getStorage().getStorageType(),
                ingredient.getCatalog() != null ? ingredient.getCatalog().getId() : null,
                ingredient.getCatalog() != null ? ingredient.getCatalog().getName() : null,
                ingredient.getCatalog() != null ? ingredient.getCatalog().getCategory() : null,
                ingredient.getName(),
                ingredient.getRegisteredAt(),
                ingredient.getExpiresAt(),
                ingredient.getExpirySourceType(),
                ingredient.getStatus(),
                ingredient.getConsumedAt(),
                ingredient.getDiscardedAt(),
                ingredient.getNote(),
                ingredient.getSourceType()
        );
    }
}
