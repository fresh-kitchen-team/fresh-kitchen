package com.example.freshkitchen.application.ingredient.dto;

import com.example.freshkitchen.application.image.port.ImageAssetUrlResolver;
import com.example.freshkitchen.domain.catalog.entity.IngredientCatalog;
import com.example.freshkitchen.domain.catalog.enums.CatalogCategory;
import com.example.freshkitchen.domain.image.entity.ImageAsset;
import com.example.freshkitchen.domain.image.entity.IngredientImage;
import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 대표이미지 5단계 폴백 체인 단위 테스트.
 * 우선순위: 사용자 대표사진 → 카탈로그 기본이미지 → 카탈로그 이모지 → 카테고리 이모지 → 전역 기본값.
 */
class RepresentativeImageResolveTest {

    private static final String DEFAULT_EMOJI = "🍽️";

    private final ImageAssetUrlResolver imageAssetUrlResolver = mock(ImageAssetUrlResolver.class);

    @Test
    void resolve_usesUserPrimaryPhoto_whenPrimaryImageExists() {
        ImageAsset primaryAsset = mock(ImageAsset.class);
        IngredientImage primary = mock(IngredientImage.class);
        when(primary.isPrimary()).thenReturn(true);
        when(primary.getImageAsset()).thenReturn(primaryAsset);
        when(imageAssetUrlResolver.resolve(primaryAsset)).thenReturn("https://cdn/user.png");
        when(imageAssetUrlResolver.resolveThumbnail(primaryAsset)).thenReturn("https://cdn/user_thumb.png");

        Ingredient ingredient = mock(Ingredient.class);
        when(ingredient.getIngredientImages()).thenReturn(setOf(primary));

        IngredientDto.RepresentativeImage result =
                IngredientDto.RepresentativeImage.resolve(ingredient, imageAssetUrlResolver);

        assertAll(
                () -> assertEquals(IngredientDto.RepresentativeImageType.PHOTO, result.type()),
                () -> assertEquals("https://cdn/user.png", result.imageUrl()),
                () -> assertEquals("https://cdn/user_thumb.png", result.thumbnailUrl()),
                () -> assertNull(result.emoji()),
                () -> assertEquals(IngredientDto.RepresentativeImageSource.USER_PHOTO, result.source())
        );
    }

    @Test
    void resolve_usesCatalogImage_whenNoPrimaryButCatalogHasDefaultImage() {
        ImageAsset catalogAsset = mock(ImageAsset.class);
        IngredientCatalog catalog = mock(IngredientCatalog.class);
        when(catalog.getDefaultImageAsset()).thenReturn(catalogAsset);
        when(imageAssetUrlResolver.resolve(catalogAsset)).thenReturn("https://cdn/catalog.png");
        // 시스템 기본 이미지는 썸네일 variant가 없어 resolveThumbnail이 원본으로 폴백
        when(imageAssetUrlResolver.resolveThumbnail(catalogAsset)).thenReturn("https://cdn/catalog.png");

        Ingredient ingredient = mock(Ingredient.class);
        when(ingredient.getIngredientImages()).thenReturn(setOf());
        when(ingredient.getCatalog()).thenReturn(catalog);

        IngredientDto.RepresentativeImage result =
                IngredientDto.RepresentativeImage.resolve(ingredient, imageAssetUrlResolver);

        assertAll(
                () -> assertEquals(IngredientDto.RepresentativeImageType.PHOTO, result.type()),
                () -> assertEquals("https://cdn/catalog.png", result.imageUrl()),
                () -> assertEquals("https://cdn/catalog.png", result.thumbnailUrl()),
                () -> assertEquals(IngredientDto.RepresentativeImageSource.CATALOG_IMAGE, result.source())
        );
    }

    @Test
    void resolve_usesCatalogEmoji_whenCatalogHasEmojiButNoImage() {
        IngredientCatalog catalog = mock(IngredientCatalog.class);
        when(catalog.getDefaultImageAsset()).thenReturn(null);
        when(catalog.getEmoji()).thenReturn("🍅");

        Ingredient ingredient = mock(Ingredient.class);
        when(ingredient.getIngredientImages()).thenReturn(setOf());
        when(ingredient.getCatalog()).thenReturn(catalog);

        IngredientDto.RepresentativeImage result =
                IngredientDto.RepresentativeImage.resolve(ingredient, imageAssetUrlResolver);

        assertAll(
                () -> assertEquals(IngredientDto.RepresentativeImageType.EMOJI, result.type()),
                () -> assertEquals("🍅", result.emoji()),
                () -> assertNull(result.imageUrl()),
                () -> assertEquals(IngredientDto.RepresentativeImageSource.CATALOG_EMOJI, result.source())
        );
        verifyNoInteractions(imageAssetUrlResolver);
    }

    @Test
    void resolve_usesCategoryEmoji_whenCatalogEmojiBlankButCategoryPresent() {
        IngredientCatalog catalog = mock(IngredientCatalog.class);
        when(catalog.getDefaultImageAsset()).thenReturn(null);
        when(catalog.getEmoji()).thenReturn("   ");

        Ingredient ingredient = mock(Ingredient.class);
        when(ingredient.getIngredientImages()).thenReturn(setOf());
        when(ingredient.getCatalog()).thenReturn(catalog);
        when(ingredient.getCategory()).thenReturn(CatalogCategory.VEGETABLE);

        IngredientDto.RepresentativeImage result =
                IngredientDto.RepresentativeImage.resolve(ingredient, imageAssetUrlResolver);

        assertAll(
                () -> assertEquals(IngredientDto.RepresentativeImageType.EMOJI, result.type()),
                () -> assertEquals(CatalogCategory.VEGETABLE.emoji(), result.emoji()),
                () -> assertEquals(IngredientDto.RepresentativeImageSource.CATEGORY_EMOJI, result.source())
        );
    }

    @Test
    void resolve_usesCategoryEmoji_whenNoCatalogButCategoryPresent() {
        Ingredient ingredient = mock(Ingredient.class);
        when(ingredient.getIngredientImages()).thenReturn(setOf());
        when(ingredient.getCatalog()).thenReturn(null);
        when(ingredient.getCategory()).thenReturn(CatalogCategory.MEAT);

        IngredientDto.RepresentativeImage result =
                IngredientDto.RepresentativeImage.resolve(ingredient, imageAssetUrlResolver);

        assertAll(
                () -> assertEquals(IngredientDto.RepresentativeImageType.EMOJI, result.type()),
                () -> assertEquals(CatalogCategory.MEAT.emoji(), result.emoji()),
                () -> assertEquals(IngredientDto.RepresentativeImageSource.CATEGORY_EMOJI, result.source())
        );
    }

    @Test
    void resolve_usesDefaultEmoji_whenNoCatalogAndNoCategory() {
        Ingredient ingredient = mock(Ingredient.class);
        when(ingredient.getIngredientImages()).thenReturn(setOf());
        when(ingredient.getCatalog()).thenReturn(null);
        when(ingredient.getCategory()).thenReturn(null);

        IngredientDto.RepresentativeImage result =
                IngredientDto.RepresentativeImage.resolve(ingredient, imageAssetUrlResolver);

        assertAll(
                () -> assertEquals(IngredientDto.RepresentativeImageType.EMOJI, result.type()),
                () -> assertEquals(DEFAULT_EMOJI, result.emoji()),
                () -> assertEquals(IngredientDto.RepresentativeImageSource.DEFAULT, result.source())
        );
    }

    @Test
    void resolve_ignoresNonPrimaryImagesAndFallsThrough() {
        IngredientImage nonPrimary = mock(IngredientImage.class);
        lenient().when(nonPrimary.isPrimary()).thenReturn(false);

        Ingredient ingredient = mock(Ingredient.class);
        when(ingredient.getIngredientImages()).thenReturn(setOf(nonPrimary));
        when(ingredient.getCatalog()).thenReturn(null);
        when(ingredient.getCategory()).thenReturn(null);

        IngredientDto.RepresentativeImage result =
                IngredientDto.RepresentativeImage.resolve(ingredient, imageAssetUrlResolver);

        assertEquals(IngredientDto.RepresentativeImageSource.DEFAULT, result.source());
        verifyNoInteractions(imageAssetUrlResolver);
    }

    @SafeVarargs
    private static <T> Set<T> setOf(T... items) {
        return new LinkedHashSet<>(Set.of(items));
    }
}
