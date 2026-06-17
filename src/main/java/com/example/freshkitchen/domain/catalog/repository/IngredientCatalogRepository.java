package com.example.freshkitchen.domain.catalog.repository;

import com.example.freshkitchen.domain.catalog.entity.IngredientCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientCatalogRepository
        extends JpaRepository<IngredientCatalog, Long>, IngredientCatalogRepositoryCustom {

    boolean existsByDefaultImageAssetId(Long defaultImageAssetId);
}
