package com.example.freshkitchen.domain.catalog.repository;

import com.example.freshkitchen.domain.catalog.entity.CategoryExpiryRule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryExpiryRuleRepository
        extends JpaRepository<CategoryExpiryRule, Long>, CategoryExpiryRuleRepositoryCustom {
}
