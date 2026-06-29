package com.example.freshkitchen.application.analytics.usecase;

import com.example.freshkitchen.application.analytics.dto.AnalyticsDto;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;

import java.util.List;

public interface ListExpiringItemsUseCase {

    List<AnalyticsDto.ExpiringItem> list(Query query);

    record Query(
            Long userId,
            Integer maxDDay,
            StorageType storageType
    ) {
    }
}
