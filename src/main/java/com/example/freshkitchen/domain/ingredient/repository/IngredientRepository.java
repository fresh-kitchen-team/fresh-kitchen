package com.example.freshkitchen.domain.ingredient.repository;

import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import com.example.freshkitchen.domain.ingredient.enums.IngredientStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface IngredientRepository extends JpaRepository<Ingredient, Long>, IngredientRepositoryCustom {
    List<Ingredient> findByUser_IdAndStatus(Long userId, IngredientStatus status);

    @Query("""
            SELECT i FROM Ingredient i
            JOIN FETCH i.user u
            WHERE i.status = :status
              AND i.expiresAt BETWEEN :from AND :to
            """)
    List<Ingredient> findExpiringWithUser(
            @Param("status") IngredientStatus status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );
}
