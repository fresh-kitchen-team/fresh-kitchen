package com.example.freshkitchen.domain.ingredient.repository;

import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import com.example.freshkitchen.domain.ingredient.enums.IngredientStatus;

import java.util.List;
import java.util.Optional;

public interface IngredientRepositoryCustom {

    Optional<Ingredient> findByIdAndUserId(Long ingredientId, Long userId);

    Optional<Ingredient> findByIdAndUserIdAndStatus(Long ingredientId, Long userId, IngredientStatus status);

    Optional<Ingredient> findDetailByIdAndUserId(Long ingredientId, Long userId);

    Optional<Ingredient> findDetailByIdAndUserIdAndStatus(Long ingredientId, Long userId, IngredientStatus status);

    List<Ingredient> findAllByUserId(Long userId);

    List<Ingredient> findAllByUserIdAndStatus(Long userId, IngredientStatus status);

    Optional<Ingredient> findByIdWithImagesForUpdate(Long ingredientId);
}
