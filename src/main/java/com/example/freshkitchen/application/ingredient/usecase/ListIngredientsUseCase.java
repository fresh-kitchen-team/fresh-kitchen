package com.example.freshkitchen.application.ingredient.usecase;

import com.example.freshkitchen.application.ingredient.dto.IngredientDto;

import java.util.List;

public interface ListIngredientsUseCase {

    List<IngredientDto.SummaryResponse> list(Query query);

    record Query(
            Long userId,
            String name
    ) {
        public Query(Long userId) {
            this(userId, null);
        }
    }
}
