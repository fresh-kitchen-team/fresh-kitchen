package com.example.freshkitchen.domain.ingredient.repository;

import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import com.example.freshkitchen.domain.ingredient.enums.ExpirySourceType;
import com.example.freshkitchen.domain.ingredient.enums.IngredientStatus;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
                left join fetch ingredient.catalog catalog
                left join fetch catalog.defaultImageAsset
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
                left join fetch ingredient.catalog catalog
                left join fetch catalog.defaultImageAsset
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
                select distinct ingredient
                from Ingredient ingredient
                join fetch ingredient.storage
                left join fetch ingredient.catalog catalog
                left join fetch catalog.defaultImageAsset
                left join fetch ingredient.ingredientImages ingredientImage
                left join fetch ingredientImage.imageAsset
                where ingredient.user.id = :userId
                  and ingredient.status = :status
                order by ingredient.id asc
                """, Ingredient.class)
                .setParameter("userId", userId)
                .setParameter("status", status)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ingredient> findAllByUserIdAndStatusAndNameContaining(Long userId, IngredientStatus status, String name) {
        String escapedName = name
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");

        return entityManager.createQuery("""
                select distinct ingredient
                from Ingredient ingredient
                join fetch ingredient.storage
                left join fetch ingredient.catalog catalog
                left join fetch catalog.defaultImageAsset
                left join fetch ingredient.ingredientImages ingredientImage
                left join fetch ingredientImage.imageAsset
                where ingredient.user.id = :userId
                  and ingredient.status = :status
                  and lower(ingredient.name) like lower(concat('%', :name, '%')) escape '\\'
                order by ingredient.id asc
                """, Ingredient.class)
                .setParameter("userId", userId)
                .setParameter("status", status)
                .setParameter("name", escapedName)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ingredient> findManualExpiringByUserId(Long userId, LocalDate today, LocalDate deadline) {
        return entityManager.createQuery("""
                select ingredient
                from Ingredient ingredient
                join fetch ingredient.storage
                left join fetch ingredient.catalog catalog
                left join fetch catalog.defaultImageAsset
                where ingredient.user.id = :userId
                  and ingredient.status = :status
                  and ingredient.expirySourceType = :expirySourceType
                  and ingredient.expiresAt between :today and :deadline
                order by ingredient.expiresAt asc, ingredient.id asc
                """, Ingredient.class)
                .setParameter("userId", userId)
                .setParameter("status", IngredientStatus.ACTIVE)
                .setParameter("expirySourceType", ExpirySourceType.MANUAL)
                .setParameter("today", today)
                .setParameter("deadline", deadline)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ingredient> findManualExpiringByUserIdAndStorageType(
            Long userId,
            LocalDate today,
            LocalDate deadline,
            StorageType storageType
    ) {
        return entityManager.createQuery("""
                select ingredient
                from Ingredient ingredient
                join fetch ingredient.storage storage
                left join fetch ingredient.catalog catalog
                left join fetch catalog.defaultImageAsset
                where ingredient.user.id = :userId
                  and ingredient.status = :status
                  and ingredient.expirySourceType = :expirySourceType
                  and ingredient.expiresAt between :today and :deadline
                  and storage.storageType = :storageType
                order by ingredient.expiresAt asc, ingredient.id asc
                """, Ingredient.class)
                .setParameter("userId", userId)
                .setParameter("status", IngredientStatus.ACTIVE)
                .setParameter("expirySourceType", ExpirySourceType.MANUAL)
                .setParameter("today", today)
                .setParameter("deadline", deadline)
                .setParameter("storageType", storageType)
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
