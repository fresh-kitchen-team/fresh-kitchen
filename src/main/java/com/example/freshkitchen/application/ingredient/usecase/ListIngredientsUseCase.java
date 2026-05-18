package com.example.freshkitchen.application.ingredient.usecase;

import com.example.freshkitchen.application.ingredient.dto.IngredientDto;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;

import java.util.List;

public interface ListIngredientsUseCase {

    List<IngredientDto.SummaryResponse> list(Query query);

    record Query(
            Long userId,
            Integer maxDDay,
            StorageType storageType
    ) {
        public Query(Long userId) {
            this(userId, null, null);
        }
    }
}
