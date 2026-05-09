package com.example.freshkitchen.application.image.usecase;

import com.example.freshkitchen.domain.image.enums.IngredientImageSourceType;

public interface AttachIngredientImageUseCase {

    Long attach(Command command);

    record Command(
            Long userId,
            Long ingredientId,
            Long imageAssetId,
            boolean primary,
            IngredientImageSourceType sourceType
    ) {
    }
}
