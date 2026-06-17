package com.example.freshkitchen.application.ingredient.service;

import com.example.freshkitchen.application.ingredient.usecase.UpdateIngredientUseCase;
import com.example.freshkitchen.domain.catalog.entity.IngredientCatalog;
import com.example.freshkitchen.domain.catalog.repository.IngredientCatalogRepository;
import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import com.example.freshkitchen.domain.ingredient.entity.Storage;
import com.example.freshkitchen.domain.ingredient.exception.IngredientErrorCode;
import com.example.freshkitchen.domain.ingredient.exception.IngredientException;
import com.example.freshkitchen.domain.ingredient.enums.IngredientStatus;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;
import com.example.freshkitchen.domain.ingredient.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateIngredientService implements UpdateIngredientUseCase {

    private final IngredientRepository ingredientRepository;
    private final IngredientCatalogRepository ingredientCatalogRepository;
    private final DefaultStorageService defaultStorageService;

    @Override
    public void update(Command command) {
        Ingredient ingredient = ingredientRepository.findByIdAndUserIdAndStatus(
                        command.ingredientId(),
                        command.userId(),
                        IngredientStatus.ACTIVE
                )
                .orElseThrow(() -> new IngredientException(IngredientErrorCode.INGREDIENT_NOT_FOUND));

        Storage storage = command.storageType() != null
                ? findStorageForUpdate(command)
                : null;

        IngredientCatalog catalog = null;
        if (command.catalogSet() && command.catalogId() != null) {
            catalog = ingredientCatalogRepository.findById(command.catalogId())
                    .orElseThrow(() -> new IngredientException(IngredientErrorCode.CATALOG_NOT_FOUND));
        }

        ingredient.apply(new Ingredient.UpdateCommand(
                storage,
                catalog,
                command.catalogSet(),
                catalog != null ? catalog.getCategory() : null,
                command.name(),
                command.registeredAt(),
                command.registeredAtSet(),
                command.expiresAt(),
                command.expiresAtSet(),
                command.expirySourceType(),
                command.note(),
                command.noteSet(),
                command.sourceType()
        ));
    }

    private Storage findStorageForUpdate(Command command) {
        StorageType storageType = command.storageType();
        return defaultStorageService.ensureDefaultStorages(command.userId()).stream()
                .filter(storage -> storage.getStorageType() == storageType)
                .findFirst()
                .orElseThrow(() -> new IngredientException(IngredientErrorCode.STORAGE_NOT_FOUND));
    }
}
