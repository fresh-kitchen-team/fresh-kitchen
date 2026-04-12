package com.example.freshkitchen.domain.ingredient.repository;

import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select distinct ingredient
            from Ingredient ingredient
            left join fetch ingredient.ingredientImages
            where ingredient.id = :ingredientId
            """)
    Optional<Ingredient> findByIdWithImagesForUpdate(@Param("ingredientId") Long ingredientId);
}
