package com.example.freshkitchen.application.ingredient.usecase;

import com.example.freshkitchen.application.ingredient.dto.response.IngredientDetailResponse;

public interface GetIngredientUseCase {

    IngredientDetailResponse get(Query query);

    record Query(
            Long ingredientId,
            Long userId
    ) {
    }
}
