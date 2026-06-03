package com.example.freshkitchen.application.image.usecase;

public interface RemoveIngredientImageUseCase {

    void remove(Command command);

    record Command(
            Long userId,
            Long ingredientId,
            Long ingredientImageId
    ) {
    }
}
