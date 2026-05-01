package com.example.freshkitchen.domain.ingredient.service;

import com.example.freshkitchen.domain.image.entity.IngredientImage;
import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import com.example.freshkitchen.domain.ingredient.repository.IngredientRepository;
import com.example.freshkitchen.global.exception.BusinessException;
import com.example.freshkitchen.global.exception.ErrorCode;
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
            throw new BusinessException(ErrorCode.INGREDIENT_ID_REQUIRED);
        }
        if (ingredientImageId == null) {
            throw new BusinessException(ErrorCode.INGREDIENT_IMAGE_ID_REQUIRED);
        }

        Ingredient ingredient = ingredientRepository.findByIdWithImagesForUpdate(ingredientId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INGREDIENT_NOT_FOUND));

        IngredientImage targetImage = ingredient.getIngredientImages().stream()
                .filter(ingredientImage -> Objects.equals(ingredientImage.getId(), ingredientImageId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INGREDIENT_IMAGE_NOT_BELONG_TO_INGREDIENT));

        ingredient.enforcePrimaryImage(targetImage);
    }
}