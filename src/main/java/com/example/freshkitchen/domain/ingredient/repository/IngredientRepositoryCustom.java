package com.example.freshkitchen.domain.ingredient.repository;

import com.example.freshkitchen.domain.ingredient.entity.Ingredient;

import java.util.List;
import java.util.Optional;

public interface IngredientRepositoryCustom {

    Optional<Ingredient> findByIdAndUserId(Long ingredientId, Long userId);

    List<Ingredient> findAllByUserId(Long userId);

    Optional<Ingredient> findByIdWithImagesForUpdate(Long ingredientId);
}
