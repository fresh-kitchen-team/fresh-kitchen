package com.example.freshkitchen.domain.ingredient.service;

import com.example.freshkitchen.domain.image.entity.IngredientImage;
import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
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
            throw new IllegalArgumentException("ingredientId must not be null");
        }
        if (ingredientImageId == null) {
            throw new IllegalArgumentException("ingredientImageId must not be null");
        }

        Ingredient ingredient = ingredientRepository.findByIdWithImagesForUpdate(ingredientId)
                .orElseThrow(() -> new IllegalArgumentException("ingredient not found"));

        IngredientImage targetImage = ingredient.getIngredientImages().stream()
                .filter(ingredientImage -> Objects.equals(ingredientImage.getId(), ingredientImageId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("ingredient image must belong to ingredient"));

        ingredient.enforcePrimaryImage(targetImage);
    }
}
