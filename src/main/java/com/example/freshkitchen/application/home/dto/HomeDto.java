package com.example.freshkitchen.application.home.dto;

import com.example.freshkitchen.domain.ingredient.enums.StorageType;

import java.time.LocalDate;
import java.util.List;

public final class HomeDto {

    private HomeDto() {
    }

    public record SummaryResponse(
            long totalCount,
            long freshCount,
            long nearExpiryCount,
            long expiredCount,
            List<StorageSummaryResponse> storages,
            List<ItemPreviewResponse> nearExpiryItems,
            List<ItemPreviewResponse> expiredItems,
            List<ItemPreviewResponse> recentItems
    ) {
    }

    public record StorageSummaryResponse(
            StorageType storage,
            String emoji,
            String name,
            long itemCount,
            String filterKey
    ) {
    }

    public record ItemPreviewResponse(
            Long id,
            String name,
            StorageType storage,
            LocalDate expiryDate,
            HomeIngredientStatus status,
            String emoji
    ) {
    }

    public enum HomeIngredientStatus {
        FRESH,
        NEAR_EXPIRY,
        EXPIRED
    }
}
