package com.example.freshkitchen.domain.catalog.repository;

import com.example.freshkitchen.domain.catalog.entity.IngredientCatalog;
import com.example.freshkitchen.domain.catalog.enums.CatalogCategory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
class IngredientCatalogRepositoryImpl implements IngredientCatalogRepositoryCustom {

    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public Optional<IngredientCatalog> findByName(String name) {
        return entityManager.createQuery("""
                select catalog
                from IngredientCatalog catalog
                where catalog.name = :name
                """, IngredientCatalog.class)
                .setParameter("name", name)
                .getResultList()
                .stream()
                .findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IngredientCatalog> findByIdWithExpiryRules(Long catalogId) {
        return entityManager.createQuery("""
                select distinct catalog
                from IngredientCatalog catalog
                left join fetch catalog.catalogExpiryRules catalogExpiryRule
                where catalog.id = :catalogId
                """, IngredientCatalog.class)
                .setParameter("catalogId", catalogId)
                .getResultList()
                .stream()
                .findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<IngredientCatalog> findAllByCategory(CatalogCategory category) {
        return entityManager.createQuery("""
                select catalog
                from IngredientCatalog catalog
                where catalog.category = :category
                order by catalog.name asc
                """, IngredientCatalog.class)
                .setParameter("category", category)
                .getResultList();
    }
}
