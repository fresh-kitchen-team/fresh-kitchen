package com.example.freshkitchen.application.ingredient.usecase;

import com.example.freshkitchen.application.ingredient.dto.IngredientDto;

import java.util.List;

public interface ListStoragesUseCase {

    List<IngredientDto.StorageSummaryResponse> list(Query query);

    record Query(
            Long userId
    ) {
    }
}
