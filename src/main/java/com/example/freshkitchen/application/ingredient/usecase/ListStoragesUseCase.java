package com.example.freshkitchen.application.ingredient.usecase;

import com.example.freshkitchen.application.ingredient.dto.response.StorageSummaryResponse;

import java.util.List;

public interface ListStoragesUseCase {

    List<StorageSummaryResponse> list(Query query);

    record Query(
            Long userId
    ) {
    }
}
