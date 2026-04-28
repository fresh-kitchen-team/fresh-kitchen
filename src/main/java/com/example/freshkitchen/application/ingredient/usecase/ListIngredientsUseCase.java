package com.example.freshkitchen.application.ingredient.usecase;

import com.example.freshkitchen.application.ingredient.dto.response.IngredientSummaryResponse;

import java.util.List;

public interface ListIngredientsUseCase {

    List<IngredientSummaryResponse> list(Query query);

    record Query(
            Long userId
    ) {
    }
}
