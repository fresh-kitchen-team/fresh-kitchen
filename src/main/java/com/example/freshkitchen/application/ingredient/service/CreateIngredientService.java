package com.example.freshkitchen.application.ingredient.service;

import com.example.freshkitchen.application.ingredient.usecase.CreateIngredientUseCase;
import com.example.freshkitchen.domain.catalog.entity.IngredientCatalog;
import com.example.freshkitchen.domain.catalog.repository.IngredientCatalogRepository;
import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import com.example.freshkitchen.domain.ingredient.entity.Storage;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;
import com.example.freshkitchen.domain.ingredient.exception.IngredientErrorCode;
import com.example.freshkitchen.domain.ingredient.exception.IngredientException;
import com.example.freshkitchen.domain.ingredient.repository.IngredientRepository;
import com.example.freshkitchen.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateIngredientService implements CreateIngredientUseCase {

    private final IngredientRepository ingredientRepository;
    private final IngredientCatalogRepository ingredientCatalogRepository;
    private final DefaultStorageService defaultStorageService;
    private final EntityManager entityManager;

    @Override
    public Long create(Command command) {
        Storage storage = resolveStorage(command.userId(), command.storageType());
        IngredientCatalog catalog = resolveCatalog(command.catalogId());
        User user = entityManager.getReference(User.class, command.userId());

        Ingredient ingredient = Ingredient.create(new Ingredient.CreateCommand(
                user,
                storage,
                catalog,
                command.name(),
                command.registeredAt(),
                command.expiresAt(),
                command.expirySourceType(),
                command.note(),
                command.sourceType()
        ));

        return ingredientRepository.save(ingredient).getId();
    }

    private Storage resolveStorage(Long userId, StorageType storageType) {
        return defaultStorageService.ensureDefaultStorages(userId).stream()
                .filter(storage -> storage.getStorageType() == storageType)
                .findFirst()
                .orElseThrow(() -> new IngredientException(IngredientErrorCode.STORAGE_NOT_FOUND));
    }

    private IngredientCatalog resolveCatalog(Long catalogId) {
        if (catalogId == null) {
            return null;
        }
        return ingredientCatalogRepository.findById(catalogId)
                .orElseThrow(() -> new IngredientException(IngredientErrorCode.CATALOG_NOT_FOUND));
    }
}
