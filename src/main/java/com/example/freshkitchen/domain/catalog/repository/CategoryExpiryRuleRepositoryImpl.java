package com.example.freshkitchen.domain.catalog.repository;

import com.example.freshkitchen.domain.catalog.entity.CategoryExpiryRule;
import com.example.freshkitchen.domain.catalog.enums.CatalogCategory;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
class CategoryExpiryRuleRepositoryImpl implements CategoryExpiryRuleRepositoryCustom {

    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public Optional<CategoryExpiryRule> findByCategoryAndStorageType(CatalogCategory category, StorageType storageType) {
        return entityManager.createQuery("""
                select categoryExpiryRule
                from CategoryExpiryRule categoryExpiryRule
                where categoryExpiryRule.category = :category
                  and categoryExpiryRule.storageType = :storageType
                """, CategoryExpiryRule.class)
                .setParameter("category", category)
                .setParameter("storageType", storageType)
                .getResultList()
                .stream()
                .findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryExpiryRule> findAllByCategory(CatalogCategory category) {
        return entityManager.createQuery("""
                select categoryExpiryRule
                from CategoryExpiryRule categoryExpiryRule
                where categoryExpiryRule.category = :category
                order by categoryExpiryRule.storageType asc
                """, CategoryExpiryRule.class)
                .setParameter("category", category)
                .getResultList();
    }
}
