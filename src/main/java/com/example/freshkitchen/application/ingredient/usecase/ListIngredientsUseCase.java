package com.example.freshkitchen.application.ingredient.usecase;

import com.example.freshkitchen.application.ingredient.dto.IngredientSummaryResult;

import java.util.List;

public interface ListIngredientsUseCase {

    List<IngredientSummaryResult> list(Query query);

    record Query(
            Long userId
    ) {
    }
}
