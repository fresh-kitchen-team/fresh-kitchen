package com.example.freshkitchen.application.ingredient.usecase;

import com.example.freshkitchen.application.ingredient.dto.IngredientDto;

public interface GetIngredientUseCase {

    IngredientDto.DetailResponse get(Query query);

    record Query(
            Long ingredientId,
            Long userId
    ) {
    }
}
