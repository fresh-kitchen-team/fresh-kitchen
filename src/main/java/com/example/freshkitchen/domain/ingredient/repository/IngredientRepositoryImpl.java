package com.example.freshkitchen.domain.ingredient.repository;

import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import com.example.freshkitchen.domain.ingredient.enums.IngredientStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
    @Transactional(readOnly = true)
    public Optional<Ingredient> findByIdAndUserIdAndStatus(Long ingredientId, Long userId, IngredientStatus status) {
        return entityManager.createQuery("""
                select ingredient
                from Ingredient ingredient
                where ingredient.id = :ingredientId
                  and ingredient.user.id = :userId
                  and ingredient.status = :status
                """, Ingredient.class)
                .setParameter("ingredientId", ingredientId)
                .setParameter("userId", userId)
                .setParameter("status", status)
                .getResultList()
                .stream()
                .findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Ingredient> findDetailByIdAndUserId(Long ingredientId, Long userId) {
        return entityManager.createQuery("""
                select distinct ingredient
                from Ingredient ingredient
                join fetch ingredient.user
                join fetch ingredient.storage
                left join fetch ingredient.catalog
                left join fetch ingredient.ingredientImages ingredientImage
                left join fetch ingredientImage.imageAsset
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
    @Transactional(readOnly = true)
    public Optional<Ingredient> findDetailByIdAndUserIdAndStatus(Long ingredientId, Long userId, IngredientStatus status) {
        return entityManager.createQuery("""
                select distinct ingredient
                from Ingredient ingredient
                join fetch ingredient.user
                join fetch ingredient.storage
                left join fetch ingredient.catalog
                left join fetch ingredient.ingredientImages ingredientImage
                left join fetch ingredientImage.imageAsset
                where ingredient.id = :ingredientId
                  and ingredient.user.id = :userId
                  and ingredient.status = :status
                """, Ingredient.class)
                .setParameter("ingredientId", ingredientId)
                .setParameter("userId", userId)
                .setParameter("status", status)
                .getResultList()
                .stream()
                .findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ingredient> findAllByUserId(Long userId) {
        return entityManager.createQuery("""
                select ingredient
                from Ingredient ingredient
                join fetch ingredient.storage
                left join fetch ingredient.catalog
                where ingredient.user.id = :userId
                order by ingredient.id asc
                """, Ingredient.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ingredient> findAllByUserIdAndStatus(Long userId, IngredientStatus status) {
        return entityManager.createQuery("""
                select ingredient
                from Ingredient ingredient
                join fetch ingredient.storage
                left join fetch ingredient.catalog
                where ingredient.user.id = :userId
                  and ingredient.status = :status
                order by ingredient.id asc
                """, Ingredient.class)
                .setParameter("userId", userId)
                .setParameter("status", status)
                .getResultList();
    }

    @Override
    @Transactional
    public Optional<Ingredient> findByIdAndUserIdAndStatusWithImagesForUpdate(
            Long ingredientId,
            Long userId,
            IngredientStatus status
    ) {
        return entityManager.createQuery("""
                select distinct ingredient
                from Ingredient ingredient
                left join fetch ingredient.ingredientImages ingredientImage
                where ingredient.id = :ingredientId
                  and ingredient.user.id = :userId
                  and ingredient.status = :status
                """, Ingredient.class)
                .setParameter("ingredientId", ingredientId)
                .setParameter("userId", userId)
                .setParameter("status", status)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList()
                .stream()
                .findFirst();
    }
}
