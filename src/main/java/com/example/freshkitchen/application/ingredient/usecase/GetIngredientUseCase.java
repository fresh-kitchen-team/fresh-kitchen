package com.example.freshkitchen.application.ingredient.usecase;

import com.example.freshkitchen.application.ingredient.dto.IngredientDetailResult;

public interface GetIngredientUseCase {

    IngredientDetailResult get(Query query);

    record Query(
            Long ingredientId,
            Long userId
    ) {
    }
}
