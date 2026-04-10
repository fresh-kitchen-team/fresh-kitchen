package com.example.freshkitchen.application.ingredient.service;

import com.example.freshkitchen.application.ingredient.dto.IngredientDefaultsResult;
import com.example.freshkitchen.application.ingredient.usecase.ResolveIngredientDefaultsUseCase;
import com.example.freshkitchen.domain.catalog.entity.CatalogExpiryRule;
import com.example.freshkitchen.domain.catalog.entity.CategoryExpiryRule;
import com.example.freshkitchen.domain.catalog.entity.IngredientCatalog;
import com.example.freshkitchen.domain.catalog.repository.CatalogExpiryRuleRepository;
import com.example.freshkitchen.domain.catalog.repository.CategoryExpiryRuleRepository;
import com.example.freshkitchen.domain.catalog.repository.IngredientCatalogRepository;
import com.example.freshkitchen.domain.ingredient.exception.IngredientErrorCode;
import com.example.freshkitchen.domain.ingredient.exception.IngredientException;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResolveIngredientDefaultsService implements ResolveIngredientDefaultsUseCase {

    private final IngredientCatalogRepository ingredientCatalogRepository;
    private final CatalogExpiryRuleRepository catalogExpiryRuleRepository;
    private final CategoryExpiryRuleRepository categoryExpiryRuleRepository;

    @Override
    public IngredientDefaultsResult resolve(Query query) {
        if (query.catalogId() != null) {
            IngredientCatalog catalog = ingredientCatalogRepository.findById(query.catalogId())
                    .orElseThrow(() -> new IngredientException(IngredientErrorCode.CATALOG_NOT_FOUND));
            StorageType resolvedStorageType = query.storageType() != null ? query.storageType() : catalog.getDefaultStorageType();
            CatalogExpiryRule catalogRule = catalogExpiryRuleRepository.findByCatalogIdAndStorageType(catalog.getId(), resolvedStorageType)
                    .orElse(null);
            if (catalogRule != null) {
                return new IngredientDefaultsResult(
                        catalog.getId(),
                        catalog.getDefaultStorageType(),
                        catalogRule.getShelfLifeDays(),
                        catalogRule.getReferenceNote()
                );
            }
            if (query.category() != null) {
                return resolveCategoryRule(catalog.getId(), catalog.getDefaultStorageType(), query.category(), resolvedStorageType);
            }
            return new IngredientDefaultsResult(catalog.getId(), catalog.getDefaultStorageType(), null, null);
        }

        if (query.category() == null || query.storageType() == null) {
            return new IngredientDefaultsResult(null, query.storageType(), null, null);
        }
        return resolveCategoryRule(null, query.storageType(), query.category(), query.storageType());
    }

    private IngredientDefaultsResult resolveCategoryRule(
            Long catalogId,
            StorageType defaultStorageType,
            com.example.freshkitchen.domain.catalog.enums.CatalogCategory category,
            StorageType storageType
    ) {
        CategoryExpiryRule categoryRule = categoryExpiryRuleRepository.findByCategoryAndStorageType(category, storageType)
                .orElse(null);
        if (categoryRule == null) {
            return new IngredientDefaultsResult(catalogId, defaultStorageType, null, null);
        }
        return new IngredientDefaultsResult(
                catalogId,
                defaultStorageType,
                categoryRule.getShelfLifeDays(),
                categoryRule.getReferenceNote()
        );
    }
}
