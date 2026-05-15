package com.example.freshkitchen.application.ingredient.service;

import com.example.freshkitchen.application.image.port.ImageAssetUrlResolver;
import com.example.freshkitchen.application.ingredient.usecase.GetIngredientUseCase;
import com.example.freshkitchen.domain.ingredient.exception.IngredientException;
import com.example.freshkitchen.domain.ingredient.enums.IngredientStatus;
import com.example.freshkitchen.domain.ingredient.repository.IngredientRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetIngredientServiceUnitTest {

    private final IngredientRepository ingredientRepository = mock(IngredientRepository.class);
    private final ImageAssetUrlResolver imageAssetUrlResolver = mock(ImageAssetUrlResolver.class);
    private final GetIngredientUseCase getIngredientUseCase =
            new GetIngredientService(ingredientRepository, imageAssetUrlResolver);

    @Test
    void get_usesActiveStatusFilterAndRejectsDiscardedIngredient() {
        when(ingredientRepository.findDetailByIdAndUserIdAndStatus(10L, 1L, IngredientStatus.ACTIVE))
                .thenReturn(Optional.empty());

        IngredientException exception = assertThrows(
                IngredientException.class,
                () -> getIngredientUseCase.get(new GetIngredientUseCase.Query(10L, 1L))
        );

        assertEquals("ingredient not found", exception.getMessage());
        verify(ingredientRepository).findDetailByIdAndUserIdAndStatus(10L, 1L, IngredientStatus.ACTIVE);
    }
}
