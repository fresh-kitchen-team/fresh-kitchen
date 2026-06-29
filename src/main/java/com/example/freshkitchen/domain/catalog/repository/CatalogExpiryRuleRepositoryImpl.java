package com.example.freshkitchen.domain.catalog.repository;

import com.example.freshkitchen.domain.catalog.entity.CatalogExpiryRule;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
class CatalogExpiryRuleRepositoryImpl implements CatalogExpiryRuleRepositoryCustom {

    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public Optional<CatalogExpiryRule> findByCatalogIdAndStorageType(Long catalogId, StorageType storageType) {
        return entityManager.createQuery("""
                select catalogExpiryRule
                from CatalogExpiryRule catalogExpiryRule
                where catalogExpiryRule.catalog.id = :catalogId
                  and catalogExpiryRule.storageType = :storageType
                """, CatalogExpiryRule.class)
                .setParameter("catalogId", catalogId)
                .setParameter("storageType", storageType)
                .getResultList()
                .stream()
                .findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogExpiryRule> findAllByCatalogId(Long catalogId) {
        return entityManager.createQuery("""
                select catalogExpiryRule
                from CatalogExpiryRule catalogExpiryRule
                where catalogExpiryRule.catalog.id = :catalogId
                order by catalogExpiryRule.storageType asc
                """, CatalogExpiryRule.class)
                .setParameter("catalogId", catalogId)
                .getResultList();
    }
}
