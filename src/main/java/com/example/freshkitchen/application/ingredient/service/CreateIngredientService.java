package com.example.freshkitchen.application.ingredient.service;

import com.example.freshkitchen.application.ingredient.dto.IngredientDto;
import com.example.freshkitchen.application.ingredient.usecase.CreateIngredientUseCase;
import com.example.freshkitchen.application.ingredient.usecase.ResolveIngredientDefaultsUseCase;
import com.example.freshkitchen.domain.catalog.entity.IngredientCatalog;
import com.example.freshkitchen.domain.catalog.enums.CatalogCategory;
import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import com.example.freshkitchen.domain.ingredient.entity.Storage;
import com.example.freshkitchen.domain.ingredient.enums.ExpirySourceType;
import com.example.freshkitchen.domain.ingredient.enums.IngredientSourceType;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;
import com.example.freshkitchen.domain.ingredient.exception.IngredientErrorCode;
import com.example.freshkitchen.domain.ingredient.exception.IngredientException;
import com.example.freshkitchen.domain.ingredient.repository.IngredientRepository;
import com.example.freshkitchen.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateIngredientService implements CreateIngredientUseCase {

    private final IngredientRepository ingredientRepository;
    private final DefaultStorageService defaultStorageService;
    private final IngredientCatalogMappingService ingredientCatalogMappingService;
    private final ResolveIngredientDefaultsUseCase resolveIngredientDefaultsUseCase;
    private final EntityManager entityManager;

    @Override
    public Long create(Command command) {
        Storage storage = resolveStorage(command.userId(), command.storageType());
        IngredientCatalog catalog = resolveCatalog(command);
        CatalogCategory category = resolveCategory(command.category(), catalog);
        ExpiryMapping expiry = resolveExpiry(command, catalog, category, storage.getStorageType());
        User user = entityManager.getReference(User.class, command.userId());

        Ingredient ingredient = Ingredient.create(new Ingredient.CreateCommand(
                user,
                storage,
                catalog,
                category,
                command.name(),
                command.registeredAt(),
                expiry.expiresAt(),
                expiry.expirySourceType(),
                command.note(),
                command.sourceType()
        ));

        return ingredientRepository.save(ingredient).getId();
    }

    private IngredientCatalog resolveCatalog(Command command) {
        if (command.catalogId() != null || isScanSource(command.sourceType())) {
            return ingredientCatalogMappingService.resolve(command.catalogId(), command.name());
        }
        return null;
    }

    private CatalogCategory resolveCategory(CatalogCategory requestedCategory, IngredientCatalog catalog) {
        if (requestedCategory != null) {
            return requestedCategory;
        }
        if (catalog != null) {
            return catalog.getCategory();
        }
        return CatalogCategory.ETC;
    }

    private Storage resolveStorage(Long userId, StorageType storageType) {
        return defaultStorageService.ensureDefaultStorages(userId).stream()
                .filter(storage -> storage.getStorageType() == storageType)
                .findFirst()
                .orElseThrow(() -> new IngredientException(IngredientErrorCode.STORAGE_NOT_FOUND));
    }

    private ExpiryMapping resolveExpiry(Command command, IngredientCatalog catalog, CatalogCategory category, StorageType storageType) {
        if (command.expiresAt() != null) {
            ExpirySourceType sourceType = command.expirySourceType() != null
                    ? command.expirySourceType()
                    : ExpirySourceType.MANUAL;
            return new ExpiryMapping(command.expiresAt(), sourceType);
        }
        if (command.registeredAt() == null) {
            return new ExpiryMapping(null, ExpirySourceType.UNKNOWN);
        }
        if (!isScanSource(command.sourceType()) && catalog == null && command.category() == null) {
            return new ExpiryMapping(null, ExpirySourceType.UNKNOWN);
        }

        IngredientDto.DefaultsResponse defaults = resolveIngredientDefaultsUseCase.resolve(
                new ResolveIngredientDefaultsUseCase.Query(catalog != null ? catalog.getId() : null, category, storageType)
        );
        if (defaults.shelfLifeDays() == null) {
            return new ExpiryMapping(null, ExpirySourceType.UNKNOWN);
        }
        return new ExpiryMapping(command.registeredAt().plusDays(defaults.shelfLifeDays()), ExpirySourceType.POLICY);
    }

    private static boolean isScanSource(IngredientSourceType sourceType) {
        return sourceType == IngredientSourceType.PHOTO || sourceType == IngredientSourceType.RECEIPT;
    }

    private record ExpiryMapping(
            LocalDate expiresAt,
            ExpirySourceType expirySourceType
    ) {
    }
}
