package com.example.freshkitchen.domain.ingredient.repository;

import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientRepository extends JpaRepository<Ingredient, Long>, IngredientRepositoryCustom {
}
