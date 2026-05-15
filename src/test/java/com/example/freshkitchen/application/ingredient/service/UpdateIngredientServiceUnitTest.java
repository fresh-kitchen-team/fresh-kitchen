package com.example.freshkitchen.application.ingredient.service;

import com.example.freshkitchen.application.ingredient.usecase.UpdateIngredientUseCase;
import com.example.freshkitchen.domain.catalog.repository.IngredientCatalogRepository;
import com.example.freshkitchen.domain.ingredient.exception.IngredientException;
import com.example.freshkitchen.domain.ingredient.enums.IngredientStatus;
import com.example.freshkitchen.domain.ingredient.repository.IngredientRepository;
import com.example.freshkitchen.domain.ingredient.repository.StorageRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdateIngredientServiceUnitTest {

    private final IngredientRepository ingredientRepository = mock(IngredientRepository.class);
    private final StorageRepository storageRepository = mock(StorageRepository.class);
    private final IngredientCatalogRepository ingredientCatalogRepository = mock(IngredientCatalogRepository.class);
    private final DefaultStorageService defaultStorageService = mock(DefaultStorageService.class);
    private final UpdateIngredientUseCase updateIngredientUseCase =
            new UpdateIngredientService(
                    ingredientRepository,
                    storageRepository,
                    ingredientCatalogRepository,
                    defaultStorageService
            );

    @Test
    void update_usesActiveStatusFilterAndRejectsDiscardedIngredient() {
        when(ingredientRepository.findByIdAndUserIdAndStatus(10L, 1L, IngredientStatus.ACTIVE))
                .thenReturn(Optional.empty());

        IngredientException exception = assertThrows(
                IngredientException.class,
                () -> updateIngredientUseCase.update(new UpdateIngredientUseCase.Command(
                        10L,
                        1L,
                        null,
                        null,
                        false,
                        "Milk",
                        null,
                        false,
                        null,
                        false,
                        null,
                        null,
                        false,
                        null
                ))
        );

        assertEquals("ingredient not found", exception.getMessage());
        verify(ingredientRepository).findByIdAndUserIdAndStatus(10L, 1L, IngredientStatus.ACTIVE);
    }
}
