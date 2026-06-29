package com.example.freshkitchen.application.image.usecase;

public interface ChangeIngredientPrimaryImageUseCase {

    void change(Command command);

    record Command(
            Long userId,
            Long ingredientId,
            Long ingredientImageId
    ) {
    }
}
