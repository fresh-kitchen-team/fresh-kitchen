package com.example.freshkitchen.application.ingredient.dto;

import com.example.freshkitchen.domain.catalog.enums.CatalogCategory;
import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import com.example.freshkitchen.domain.ingredient.entity.Storage;
import com.example.freshkitchen.domain.ingredient.enums.ExpirySourceType;
import com.example.freshkitchen.domain.ingredient.enums.IngredientSourceType;
import com.example.freshkitchen.domain.ingredient.enums.IngredientStatus;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;

import java.time.LocalDate;

public final class IngredientDto {

    private IngredientDto() {
    }

    public record SummaryResponse(
            Long ingredientId,
            String name,
            IngredientStatus status,
            Long storageId,
            String storageName,
            StorageType storageType,
            Long catalogId,
            LocalDate expiresAt
    ) {

        public static SummaryResponse from(Ingredient ingredient) {
            return new SummaryResponse(
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

    public record DetailResponse(
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

        public static DetailResponse from(Ingredient ingredient) {
            return new DetailResponse(
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

    public record DefaultsResponse(
            Long catalogId,
            StorageType defaultStorageType,
            Integer shelfLifeDays,
            String referenceNote
    ) {
    }

    public record StorageSummaryResponse(
            Long storageId,
            StorageType storageType,
            String name
    ) {

        public static StorageSummaryResponse from(Storage storage) {
            return new StorageSummaryResponse(
                    storage.getId(),
                    storage.getStorageType(),
                    storage.getName()
            );
        }
    }
}
