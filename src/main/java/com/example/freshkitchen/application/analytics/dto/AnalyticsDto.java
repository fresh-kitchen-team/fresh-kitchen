package com.example.freshkitchen.application.analytics.dto;

import com.example.freshkitchen.domain.ingredient.enums.StorageType;

import java.time.LocalDate;
import java.util.List;

public final class AnalyticsDto {

    private AnalyticsDto() {
    }

    public record SummaryResponse(
            int totalActiveCount,
            int urgentCount,
            DisplayCategory topUrgentCategory,
            String topUrgentCategoryDisplayName,
            int topUrgentCategoryCount,
            String message,
            double overallDiscardRate,
            List<CategoryStat> categoryStats,
            List<UrgentItem> urgentItems
    ) {
    }

    public record CategoryStat(
            DisplayCategory category,
            String displayName,
            int activeCount,
            int urgentCount,
            double discardRate
    ) {
    }

    public record UrgentItem(
            Long id,
            String name,
            String emoji,
            LocalDate expiresAt,
            int dDay,
            StorageType storageType
    ) {
    }

    public record ExpiringItem(
            Long id,
            String name,
            String emoji,
            DisplayCategory category,
            String categoryDisplayName,
            LocalDate expiresAt,
            int dDay,
            StorageType storageType
    ) {
    }
}
