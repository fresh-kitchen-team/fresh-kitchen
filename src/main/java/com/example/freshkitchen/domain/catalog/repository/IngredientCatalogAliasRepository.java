package com.example.freshkitchen.domain.catalog.repository;

import com.example.freshkitchen.domain.catalog.entity.IngredientCatalogAlias;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IngredientCatalogAliasRepository extends JpaRepository<IngredientCatalogAlias, Long> {

    Optional<IngredientCatalogAlias> findByNormalizedAliasName(String normalizedAliasName);
}
