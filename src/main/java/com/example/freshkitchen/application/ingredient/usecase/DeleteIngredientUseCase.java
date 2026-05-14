package com.example.freshkitchen.application.ingredient.usecase;

public interface DeleteIngredientUseCase {

    void delete(Command command);

    record Command(
            Long ingredientId,
            Long userId
    ) {
    }
}
