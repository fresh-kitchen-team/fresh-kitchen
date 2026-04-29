package com.example.freshkitchen.domain.catalog.repository;

import com.example.freshkitchen.domain.catalog.entity.CatalogExpiryRule;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;

import java.util.List;
import java.util.Optional;

public interface CatalogExpiryRuleRepositoryCustom {

    Optional<CatalogExpiryRule> findByCatalogIdAndStorageType(Long catalogId, StorageType storageType);

    List<CatalogExpiryRule> findAllByCatalogId(Long catalogId);
}
