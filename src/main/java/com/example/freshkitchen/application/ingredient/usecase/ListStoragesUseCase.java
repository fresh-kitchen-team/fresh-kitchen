package com.example.freshkitchen.application.ingredient.usecase;

import com.example.freshkitchen.application.ingredient.dto.StorageSummaryResult;

import java.util.List;

public interface ListStoragesUseCase {

    List<StorageSummaryResult> list(Query query);

    record Query(
            Long userId
    ) {
    }
}
