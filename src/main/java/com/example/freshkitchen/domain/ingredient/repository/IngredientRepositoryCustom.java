package com.example.freshkitchen.domain.ingredient.repository;

import com.example.freshkitchen.domain.ingredient.entity.Ingredient;

import java.util.Optional;

public interface IngredientRepositoryCustom {

    Optional<Ingredient> findByIdAndUserId(Long ingredientId, Long userId);

    Optional<Ingredient> findByIdWithImagesForUpdate(Long ingredientId);
}
