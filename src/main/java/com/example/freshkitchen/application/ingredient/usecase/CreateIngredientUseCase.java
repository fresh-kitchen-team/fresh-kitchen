package com.example.freshkitchen.application.ingredient.usecase;

import com.example.freshkitchen.domain.ingredient.enums.ExpirySourceType;
import com.example.freshkitchen.domain.ingredient.enums.IngredientSourceType;

import java.time.LocalDate;

public interface CreateIngredientUseCase {

    Long create(Command command);

    record Command(
            Long userId,
            Long storageId,
            Long catalogId,
            String name,
            LocalDate registeredAt,
            LocalDate expiresAt,
            ExpirySourceType expirySourceType,
            String note,
            IngredientSourceType sourceType
    ) {
    }
}
