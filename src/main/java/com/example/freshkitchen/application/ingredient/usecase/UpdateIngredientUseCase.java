package com.example.freshkitchen.application.ingredient.usecase;

import com.example.freshkitchen.domain.ingredient.enums.ExpirySourceType;
import com.example.freshkitchen.domain.ingredient.enums.IngredientSourceType;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;

import java.time.LocalDate;

public interface UpdateIngredientUseCase {

    void update(Command command);

    record Command(
            Long ingredientId,
            Long userId,
            StorageType storageType,
            Long catalogId,
            boolean catalogSet,
            String name,
            LocalDate registeredAt,
            boolean registeredAtSet,
            LocalDate expiresAt,
            boolean expiresAtSet,
            ExpirySourceType expirySourceType,
            String note,
            boolean noteSet,
            IngredientSourceType sourceType
    ) {
    }
}
