package com.example.freshkitchen.application.image.service;

import com.example.freshkitchen.application.image.usecase.ChangeIngredientPrimaryImageUseCase;
import com.example.freshkitchen.domain.image.entity.IngredientImage;
import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import com.example.freshkitchen.domain.ingredient.enums.IngredientStatus;
import com.example.freshkitchen.domain.ingredient.exception.IngredientErrorCode;
import com.example.freshkitchen.domain.ingredient.exception.IngredientException;
import com.example.freshkitchen.domain.ingredient.repository.IngredientRepository;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ChangeIngredientPrimaryImageService implements ChangeIngredientPrimaryImageUseCase {

    private final IngredientRepository ingredientRepository;

    @Override
    public void change(Command command) {
        validate(command);
        Ingredient ingredient = ingredientRepository.findByIdAndUserIdAndStatusWithImagesForUpdate(
                        command.ingredientId(),
                        command.userId(),
                        IngredientStatus.ACTIVE
                )
                .orElseThrow(() -> new IngredientException(IngredientErrorCode.INGREDIENT_NOT_FOUND));

        IngredientImage targetImage = ingredient.getIngredientImages().stream()
                .filter(ingredientImage -> ingredientImage.getId().equals(command.ingredientImageId()))
                .findFirst()
                .orElseThrow(() -> new IngredientException(IngredientErrorCode.INGREDIENT_IMAGE_NOT_BELONG_TO_INGREDIENT));

        ingredient.enforcePrimaryImage(targetImage);
    }

    private static void validate(Command command) {
        if (command == null) {
            throw new BusinessValidationException("command must not be null");
        }
        if (command.userId() == null) {
            throw new BusinessValidationException("userId must not be null");
        }
        if (command.ingredientId() == null) {
            throw new IngredientException(IngredientErrorCode.INGREDIENT_ID_REQUIRED);
        }
        if (command.ingredientImageId() == null) {
            throw new IngredientException(IngredientErrorCode.INGREDIENT_IMAGE_ID_REQUIRED);
        }
    }
}
