package com.example.freshkitchen.domain.catalog.repository;

import com.example.freshkitchen.domain.catalog.entity.CatalogExpiryRule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogExpiryRuleRepository
        extends JpaRepository<CatalogExpiryRule, Long>, CatalogExpiryRuleRepositoryCustom {
}
