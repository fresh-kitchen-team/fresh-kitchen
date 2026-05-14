package com.example.freshkitchen.application.ingredient.service;

import com.example.freshkitchen.application.ingredient.usecase.UpdateIngredientUseCase;
import com.example.freshkitchen.domain.catalog.entity.IngredientCatalog;
import com.example.freshkitchen.domain.catalog.repository.IngredientCatalogRepository;
import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import com.example.freshkitchen.domain.ingredient.entity.Storage;
import com.example.freshkitchen.domain.ingredient.exception.IngredientErrorCode;
import com.example.freshkitchen.domain.ingredient.exception.IngredientException;
import com.example.freshkitchen.domain.ingredient.repository.IngredientRepository;
import com.example.freshkitchen.domain.ingredient.repository.StorageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateIngredientService implements UpdateIngredientUseCase {

    private final IngredientRepository ingredientRepository;
    private final StorageRepository storageRepository;
    private final IngredientCatalogRepository ingredientCatalogRepository;
    private final DefaultStorageService defaultStorageService;

    @Override
    public void update(Command command) {
        Ingredient ingredient = ingredientRepository.findByIdAndUserId(command.ingredientId(), command.userId())
                .orElseThrow(() -> new IngredientException(IngredientErrorCode.INGREDIENT_NOT_FOUND));

        Storage storage = command.storageId() != null
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
        defaultStorageService.ensureDefaultStorages(command.userId());
        return storageRepository.findByIdAndUserId(command.storageId(), command.userId())
                .orElseThrow(() -> new IngredientException(IngredientErrorCode.STORAGE_NOT_FOUND));
    }
}
