package com.example.freshkitchen.domain.ingredient.repository;

import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@RequiredArgsConstructor
class IngredientRepositoryImpl implements IngredientRepositoryCustom {

    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public Optional<Ingredient> findByIdAndUserId(Long ingredientId, Long userId) {
        return entityManager.createQuery("""
                select ingredient
                from Ingredient ingredient
                where ingredient.id = :ingredientId
                  and ingredient.user.id = :userId
                """, Ingredient.class)
                .setParameter("ingredientId", ingredientId)
                .setParameter("userId", userId)
                .getResultList()
                .stream()
                .findFirst();
    }

    @Override
    @Transactional
    public Optional<Ingredient> findByIdWithImagesForUpdate(Long ingredientId) {
        return entityManager.createQuery("""
                select distinct ingredient
                from Ingredient ingredient
                left join fetch ingredient.ingredientImages ingredientImage
                where ingredient.id = :ingredientId
                """, Ingredient.class)
                .setParameter("ingredientId", ingredientId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList()
                .stream()
                .findFirst();
    }
}
