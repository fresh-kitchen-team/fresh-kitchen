package com.example.freshkitchen.application.ingredient.usecase;

public interface CreateIngredientWithImageUseCase {

    Long create(Command command);

    record Command(
            CreateIngredientUseCase.Command ingredientCommand,
            Long imageAssetId
    ) {
    }
}
