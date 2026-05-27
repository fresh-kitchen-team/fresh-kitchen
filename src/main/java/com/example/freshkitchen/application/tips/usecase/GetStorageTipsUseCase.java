package com.example.freshkitchen.application.tips.usecase;

import com.example.freshkitchen.application.analytics.dto.DisplayCategory;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;

import java.util.List;

public interface GetStorageTipsUseCase {

    List<StorageTipResult> get(Query query);

    record Query(DisplayCategory displayCategory) {
    }

    record StorageTipResult(
            Long id,
            DisplayCategory displayCategory,
            String displayCategoryName,
            String name,
            String emoji,
            String tip,
            StorageType storageType
    ) {
    }
}
