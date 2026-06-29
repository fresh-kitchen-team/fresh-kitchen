package com.example.freshkitchen.application.image.usecase;

import com.example.freshkitchen.domain.image.enums.IngredientImageSourceType;

public interface UploadIngredientImageUseCase {

    Result upload(Command command);

    record Command(
            Long userId,
            Long ingredientId,
            String originalFilename,
            String contentType,
            byte[] content,
            boolean primary,
            IngredientImageSourceType sourceType
    ) {
    }

    record Result(
            Long ingredientImageId
    ) {
    }
}
