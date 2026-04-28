package com.example.freshkitchen.domain.ingredient.repository;

import com.example.freshkitchen.domain.ingredient.entity.Storage;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
class StorageRepositoryImpl implements StorageRepositoryCustom {

    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public Optional<Storage> findByIdAndUserId(Long storageId, Long userId) {
        return entityManager.createQuery("""
                select storage
                from Storage storage
                join fetch storage.user
                where storage.id = :storageId
                  and storage.user.id = :userId
                """, Storage.class)
                .setParameter("storageId", storageId)
                .setParameter("userId", userId)
                .getResultList()
                .stream()
                .findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Storage> findByIdAndUserIdWithIngredients(Long storageId, Long userId) {
        return entityManager.createQuery("""
                select distinct storage
                from Storage storage
                left join fetch storage.ingredients ingredient
                where storage.id = :storageId
                  and storage.user.id = :userId
                """, Storage.class)
                .setParameter("storageId", storageId)
                .setParameter("userId", userId)
                .getResultList()
                .stream()
                .findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Storage> findAllByUserId(Long userId) {
        return entityManager.createQuery("""
                select storage
                from Storage storage
                where storage.user.id = :userId
                order by storage.id asc
                """, Storage.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StorageType> findStorageTypesByUserId(Long userId) {
        return entityManager.createQuery("""
                select storage.storageType
                from Storage storage
                where storage.user.id = :userId
                """, StorageType.class)
                .setParameter("userId", userId)
                .getResultList();
    }
}
