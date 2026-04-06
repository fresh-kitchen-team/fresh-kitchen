package com.example.freshkitchen.domain.ingredient.service;

import com.example.freshkitchen.domain.image.entity.IngredientImage;
import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import com.example.freshkitchen.domain.ingredient.exception.IngredientErrorCode;
import com.example.freshkitchen.domain.ingredient.exception.IngredientException;
import com.example.freshkitchen.domain.ingredient.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class IngredientImagePrimaryService {

    private final IngredientRepository ingredientRepository;

    @Transactional
    public void changePrimaryImage(Long ingredientId, Long ingredientImageId) {
        if (ingredientId == null) {
            throw new IngredientException(IngredientErrorCode.INGREDIENT_ID_REQUIRED);
        }
        if (ingredientImageId == null) {
            throw new IngredientException(IngredientErrorCode.INGREDIENT_IMAGE_ID_REQUIRED);
        }

        Ingredient ingredient = ingredientRepository.findByIdWithImagesForUpdate(ingredientId)
                .orElseThrow(() -> new IngredientException(IngredientErrorCode.INGREDIENT_NOT_FOUND));

        IngredientImage targetImage = ingredient.getIngredientImages().stream()
                .filter(ingredientImage -> Objects.equals(ingredientImage.getId(), ingredientImageId))
                .findFirst()
                .orElseThrow(() -> new IngredientException(IngredientErrorCode.INGREDIENT_IMAGE_NOT_BELONG_TO_INGREDIENT));

        ingredient.enforcePrimaryImage(targetImage);
    }
}
