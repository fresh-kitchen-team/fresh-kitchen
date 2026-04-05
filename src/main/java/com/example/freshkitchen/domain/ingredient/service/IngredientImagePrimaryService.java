package com.example.freshkitchen.domain.ingredient.service;

import com.example.freshkitchen.domain.image.entity.IngredientImage;
import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import com.example.freshkitchen.domain.ingredient.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IngredientImagePrimaryService {

    private final IngredientRepository ingredientRepository;

    @Transactional
    public void changePrimaryImage(Long ingredientId, Long ingredientImageId) {
        Ingredient ingredient = ingredientRepository.findByIdWithImagesForUpdate(ingredientId)
                .orElseThrow(() -> new IllegalArgumentException("ingredient not found"));

        IngredientImage primaryImage = ingredient.getIngredientImages().stream()
                .filter(ingredientImage -> ingredientImage.getId().equals(ingredientImageId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("ingredient image must belong to ingredient"));

        ingredient.enforcePrimaryImage(primaryImage);
    }
}
