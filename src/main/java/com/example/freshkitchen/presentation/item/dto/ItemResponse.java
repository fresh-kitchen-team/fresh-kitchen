package com.example.freshkitchen.presentation.item.dto;

import com.example.freshkitchen.application.ingredient.dto.IngredientDto;
import com.example.freshkitchen.domain.catalog.enums.CatalogCategory;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

public final class ItemResponse {

    private static final int NEAR_EXPIRY_DAYS = 7;
    private static final String DEFAULT_EMOJI = "🍽️";

    private ItemResponse() {
    }

    public record Create(
            Long id
    ) {
    }

    public record Consume(
            LocalDate consumedAt
    ) {
    }

    public record Item(
            Long id,
            String name,
            FreshnessStatus status,
            Long catalogId,
            Long storageId,
            StorageType storage,
            CatalogCategory category,
            LocalDate expiryDate,
            String emoji,
            LocalDate purchaseDate,
            String memo
    ) {

        public static Item from(IngredientDto.SummaryResponse response, Clock clock) {
            return new Item(
                    response.ingredientId(),
                    response.name(),
                    FreshnessStatus.from(response.expiresAt(), LocalDate.now(clock)),
                    response.catalogId(),
                    response.storageId(),
                    response.storageType(),
                    response.catalogCategory(),
                    response.expiresAt(),
                    resolveEmoji(response.emoji()),
                    response.registeredAt(),
                    response.note()
            );
        }

        public static Item from(IngredientDto.DetailResponse response, Clock clock) {
            return new Item(
                    response.ingredientId(),
                    response.name(),
                    FreshnessStatus.from(response.expiresAt(), LocalDate.now(clock)),
                    response.catalogId(),
                    response.storageId(),
                    response.storageType(),
                    response.catalogCategory(),
                    response.expiresAt(),
                    resolveEmoji(response.emoji()),
                    response.registeredAt(),
                    response.note()
            );
        }
    }

    public enum FreshnessStatus {
        FRESH,
        NEAR_EXPIRY,
        EXPIRED;

        private static FreshnessStatus from(LocalDate expiryDate, LocalDate today) {
            if (expiryDate == null) {
                return FRESH;
            }
            if (expiryDate.isBefore(today)) {
                return EXPIRED;
            }
            if (!expiryDate.isAfter(today.plusDays(NEAR_EXPIRY_DAYS))) {
                return NEAR_EXPIRY;
            }
            return FRESH;
        }
    }

    public static List<Item> fromSummaries(List<IngredientDto.SummaryResponse> responses, Clock clock) {
        return responses.stream()
                .map(response -> Item.from(response, clock))
                .toList();
    }

    private static String resolveEmoji(String emoji) {
        if (emoji == null || emoji.isBlank()) {
            return DEFAULT_EMOJI;
        }
        return emoji;
    }
}
