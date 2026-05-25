package com.example.freshkitchen.application.ingredient.usecase;

import com.example.freshkitchen.domain.catalog.enums.CatalogCategory;
import com.example.freshkitchen.domain.ingredient.enums.ExpirySourceType;
import com.example.freshkitchen.domain.ingredient.enums.IngredientSourceType;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;

import java.time.LocalDate;

public interface CreateIngredientUseCase {

    Long create(Command command);

    record Command(
            Long userId,
            StorageType storageType,
            Long catalogId,
            CatalogCategory category,
            String name,
            LocalDate registeredAt,
            LocalDate expiresAt,
            ExpirySourceType expirySourceType,
            String note,
            IngredientSourceType sourceType
    ) {
        public Command(
                Long userId,
                StorageType storageType,
                Long catalogId,
                String name,
                LocalDate registeredAt,
                LocalDate expiresAt,
                ExpirySourceType expirySourceType,
                String note,
                IngredientSourceType sourceType
        ) {
            this(userId, storageType, catalogId, null, name, registeredAt, expiresAt, expirySourceType, note, sourceType);
        }
    }
}
