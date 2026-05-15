package com.example.freshkitchen.application.ingredient.dto;

import com.example.freshkitchen.application.image.port.ImageAssetUrlResolver;
import com.example.freshkitchen.domain.catalog.enums.CatalogCategory;
import com.example.freshkitchen.domain.image.entity.ImageAsset;
import com.example.freshkitchen.domain.image.entity.IngredientImage;
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
            CatalogCategory catalogCategory,
            String emoji,
            LocalDate expiresAt,
            LocalDate registeredAt,
            String note,
            ImageResponse primaryImage
    ) {

        public static SummaryResponse from(Ingredient ingredient, ImageAssetUrlResolver imageAssetUrlResolver) {
            return new SummaryResponse(
                    ingredient.getId(),
                    ingredient.getName(),
                    ingredient.getStatus(),
                    ingredient.getStorage().getId(),
                    ingredient.getStorage().getName(),
                    ingredient.getStorage().getStorageType(),
                    ingredient.getCatalog() != null ? ingredient.getCatalog().getId() : null,
                    ingredient.getCatalog() != null ? ingredient.getCatalog().getCategory() : null,
                    ingredient.getCatalog() != null ? ingredient.getCatalog().getEmoji() : null,
                    ingredient.getExpiresAt(),
                    ingredient.getRegisteredAt(),
                    ingredient.getNote(),
                    ImageResponse.primaryFrom(ingredient, imageAssetUrlResolver)
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
            String emoji,
            String name,
            LocalDate registeredAt,
            LocalDate expiresAt,
            ExpirySourceType expirySourceType,
            IngredientStatus status,
            LocalDate consumedAt,
            LocalDate discardedAt,
            String note,
            IngredientSourceType sourceType,
            ImageResponse primaryImage
    ) {

        public static DetailResponse from(Ingredient ingredient, ImageAssetUrlResolver imageAssetUrlResolver) {
            return new DetailResponse(
                    ingredient.getId(),
                    ingredient.getUser().getId(),
                    ingredient.getStorage().getId(),
                    ingredient.getStorage().getName(),
                    ingredient.getStorage().getStorageType(),
                    ingredient.getCatalog() != null ? ingredient.getCatalog().getId() : null,
                    ingredient.getCatalog() != null ? ingredient.getCatalog().getName() : null,
                    ingredient.getCatalog() != null ? ingredient.getCatalog().getCategory() : null,
                    ingredient.getCatalog() != null ? ingredient.getCatalog().getEmoji() : null,
                    ingredient.getName(),
                    ingredient.getRegisteredAt(),
                    ingredient.getExpiresAt(),
                    ingredient.getExpirySourceType(),
                    ingredient.getStatus(),
                    ingredient.getConsumedAt(),
                    ingredient.getDiscardedAt(),
                    ingredient.getNote(),
                    ingredient.getSourceType(),
                    ImageResponse.primaryFrom(ingredient, imageAssetUrlResolver)
            );
        }
    }

    public record ImageResponse(
            Long ingredientImageId,
            Long imageAssetId,
            String imageUrl
    ) {

        private static ImageResponse primaryFrom(
                Ingredient ingredient,
                ImageAssetUrlResolver imageAssetUrlResolver
        ) {
            return ingredient.getIngredientImages().stream()
                    .filter(IngredientImage::isPrimary)
                    .findFirst()
                    .map(ingredientImage -> from(ingredientImage, imageAssetUrlResolver))
                    .orElse(null);
        }

        private static ImageResponse from(
                IngredientImage ingredientImage,
                ImageAssetUrlResolver imageAssetUrlResolver
        ) {
            ImageAsset imageAsset = ingredientImage.getImageAsset();
            return new ImageResponse(
                    ingredientImage.getId(),
                    imageAsset.getId(),
                    imageAssetUrlResolver.resolve(imageAsset)
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
