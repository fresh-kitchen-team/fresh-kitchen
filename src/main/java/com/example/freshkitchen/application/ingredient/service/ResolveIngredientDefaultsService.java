package com.example.freshkitchen.application.ingredient.service;

import com.example.freshkitchen.application.ingredient.dto.response.IngredientDefaultsResponse;
import com.example.freshkitchen.application.ingredient.usecase.ResolveIngredientDefaultsUseCase;
import com.example.freshkitchen.domain.catalog.enums.CatalogCategory;
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
    public IngredientDefaultsResponse resolve(Query query) {
        if (query.catalogId() != null) {
            IngredientCatalog catalog = ingredientCatalogRepository.findById(query.catalogId())
                    .orElseThrow(() -> new IngredientException(IngredientErrorCode.CATALOG_NOT_FOUND));
            StorageType resolvedStorageType = query.storageType() != null ? query.storageType() : catalog.getDefaultStorageType();
            CatalogExpiryRule catalogRule = catalogExpiryRuleRepository.findByCatalogIdAndStorageType(catalog.getId(), resolvedStorageType)
                    .orElse(null);
            if (catalogRule != null) {
                return new IngredientDefaultsResponse(
                        catalog.getId(),
                        catalog.getDefaultStorageType(),
                        catalogRule.getShelfLifeDays(),
                        catalogRule.getReferenceNote()
                );
            }
            return resolveCategoryRule(catalog.getId(), catalog.getDefaultStorageType(), catalog.getCategory(), resolvedStorageType);
        }

        if (query.category() == null || query.storageType() == null) {
            return new IngredientDefaultsResponse(null, query.storageType(), null, null);
        }
        return resolveCategoryRule(null, query.storageType(), query.category(), query.storageType());
    }

    private IngredientDefaultsResponse resolveCategoryRule(
            Long catalogId,
            StorageType defaultStorageType,
            CatalogCategory category,
            StorageType storageType
    ) {
        CategoryExpiryRule categoryRule = categoryExpiryRuleRepository.findByCategoryAndStorageType(category, storageType)
                .orElse(null);
        if (categoryRule == null) {
            return new IngredientDefaultsResponse(catalogId, defaultStorageType, null, null);
        }
        return new IngredientDefaultsResponse(
                catalogId,
                defaultStorageType,
                categoryRule.getShelfLifeDays(),
                categoryRule.getReferenceNote()
        );
    }
}
