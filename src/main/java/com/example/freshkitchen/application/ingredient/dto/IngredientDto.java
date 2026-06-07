package com.example.freshkitchen.application.ingredient.dto;

import com.example.freshkitchen.application.image.port.ImageAssetUrlResolver;
import com.example.freshkitchen.domain.catalog.entity.IngredientCatalog;
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

    private static final CatalogCategory DEFAULT_CATEGORY = CatalogCategory.ETC;
    private static final String DEFAULT_EMOJI = "🍽️";

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
            ImageResponse primaryImage,
            RepresentativeImage representativeImage
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
                    ingredient.getCategory() != null ? ingredient.getCategory() : DEFAULT_CATEGORY,
                    resolveEmoji(ingredient.getCatalog() != null ? ingredient.getCatalog().getEmoji() : null),
                    ingredient.getExpiresAt(),
                    ingredient.getRegisteredAt(),
                    ingredient.getNote(),
                    ImageResponse.primaryFrom(ingredient, imageAssetUrlResolver),
                    RepresentativeImage.resolve(ingredient, imageAssetUrlResolver)
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
            ImageResponse primaryImage,
            RepresentativeImage representativeImage
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
                    ingredient.getCategory() != null ? ingredient.getCategory() : DEFAULT_CATEGORY,
                    resolveEmoji(ingredient.getCatalog() != null ? ingredient.getCatalog().getEmoji() : null),
                    ingredient.getName(),
                    ingredient.getRegisteredAt(),
                    ingredient.getExpiresAt(),
                    ingredient.getExpirySourceType(),
                    ingredient.getStatus(),
                    ingredient.getConsumedAt(),
                    ingredient.getDiscardedAt(),
                    ingredient.getNote(),
                    ingredient.getSourceType(),
                    ImageResponse.primaryFrom(ingredient, imageAssetUrlResolver),
                    RepresentativeImage.resolve(ingredient, imageAssetUrlResolver)
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

    public enum RepresentativeImageType {
        PHOTO,
        EMOJI
    }

    public enum RepresentativeImageSource {
        USER_PHOTO,
        CATALOG_IMAGE,
        CATALOG_EMOJI,
        CATEGORY_EMOJI,
        DEFAULT
    }

    /**
     * 식재료를 UI에 노출하기 위한 단일 대표 이미지 계약.
     * type=PHOTO 이면 imageUrl, type=EMOJI 이면 emoji 가 채워진다.
     * 우선순위: 사용자 대표사진 → 카탈로그 기본이미지 → 카탈로그 이모지 → 카테고리 이모지 → 전역 기본값.
     */
    public record RepresentativeImage(
            RepresentativeImageType type,
            String imageUrl,
            String thumbnailUrl,
            String emoji,
            RepresentativeImageSource source
    ) {

        static RepresentativeImage resolve(Ingredient ingredient, ImageAssetUrlResolver imageAssetUrlResolver) {
            IngredientImage primary = ingredient.getIngredientImages().stream()
                    .filter(IngredientImage::isPrimary)
                    .findFirst()
                    .orElse(null);
            if (primary != null) {
                return photo(primary.getImageAsset(), imageAssetUrlResolver,
                        RepresentativeImageSource.USER_PHOTO);
            }

            IngredientCatalog catalog = ingredient.getCatalog();
            if (catalog != null && catalog.getDefaultImageAsset() != null) {
                return photo(catalog.getDefaultImageAsset(), imageAssetUrlResolver,
                        RepresentativeImageSource.CATALOG_IMAGE);
            }
            if (catalog != null && catalog.getEmoji() != null && !catalog.getEmoji().isBlank()) {
                return emoji(catalog.getEmoji(), RepresentativeImageSource.CATALOG_EMOJI);
            }

            CatalogCategory category = ingredient.getCategory();
            if (category != null) {
                return emoji(category.emoji(), RepresentativeImageSource.CATEGORY_EMOJI);
            }
            return emoji(DEFAULT_EMOJI, RepresentativeImageSource.DEFAULT);
        }

        private static RepresentativeImage photo(
                ImageAsset imageAsset,
                ImageAssetUrlResolver imageAssetUrlResolver,
                RepresentativeImageSource source
        ) {
            return new RepresentativeImage(
                    RepresentativeImageType.PHOTO,
                    imageAssetUrlResolver.resolve(imageAsset),
                    imageAssetUrlResolver.resolveThumbnail(imageAsset),
                    null,
                    source
            );
        }

        private static RepresentativeImage emoji(String emoji, RepresentativeImageSource source) {
            return new RepresentativeImage(RepresentativeImageType.EMOJI, null, null, emoji, source);
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

    private static String resolveEmoji(String emoji) {
        if (emoji == null || emoji.isBlank()) {
            return DEFAULT_EMOJI;
        }
        return emoji;
    }
}
