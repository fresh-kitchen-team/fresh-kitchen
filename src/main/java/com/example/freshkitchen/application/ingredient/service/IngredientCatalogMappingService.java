package com.example.freshkitchen.application.ingredient.service;

import com.example.freshkitchen.domain.catalog.entity.IngredientCatalog;
import com.example.freshkitchen.domain.catalog.repository.IngredientCatalogRepository;
import com.example.freshkitchen.domain.ingredient.exception.IngredientErrorCode;
import com.example.freshkitchen.domain.ingredient.exception.IngredientException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IngredientCatalogMappingService {

    private final IngredientCatalogRepository ingredientCatalogRepository;

    public IngredientCatalog resolve(Long catalogId, String name) {
        if (catalogId != null) {
            return ingredientCatalogRepository.findById(catalogId)
                    .orElseThrow(() -> new IngredientException(IngredientErrorCode.CATALOG_NOT_FOUND));
        }

        String normalizedName = normalizeName(name);
        if (normalizedName == null) {
            return null;
        }
        return ingredientCatalogRepository.findByName(normalizedName)
                .orElse(null);
    }

    private static String normalizeName(String name) {
        if (name == null) {
            return null;
        }
        String normalizedName = name.trim();
        if (normalizedName.isBlank()) {
            return null;
        }
        return normalizedName;
    }
}
