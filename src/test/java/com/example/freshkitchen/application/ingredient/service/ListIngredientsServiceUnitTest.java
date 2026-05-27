package com.example.freshkitchen.application.ingredient.service;

import com.example.freshkitchen.application.image.port.ImageAssetUrlResolver;
import com.example.freshkitchen.application.ingredient.dto.IngredientDto;
import com.example.freshkitchen.application.ingredient.usecase.ListIngredientsUseCase;
import com.example.freshkitchen.domain.ingredient.enums.IngredientStatus;
import com.example.freshkitchen.domain.ingredient.repository.IngredientRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

class ListIngredientsServiceUnitTest {

    private final IngredientRepository ingredientRepository = mock(IngredientRepository.class);
    private final ImageAssetUrlResolver imageAssetUrlResolver = mock(ImageAssetUrlResolver.class);
    private final ListIngredientsUseCase listIngredientsUseCase =
            new ListIngredientsService(ingredientRepository, imageAssetUrlResolver);

    @Test
    void list_withName_callsNameContainingSearch() {
        when(ingredientRepository.findAllByUserIdAndStatusAndNameContaining(1L, IngredientStatus.ACTIVE, "토마토"))
                .thenReturn(List.of());

        List<IngredientDto.SummaryResponse> results =
                listIngredientsUseCase.list(new ListIngredientsUseCase.Query(1L, "토마토"));

        assertTrue(results.isEmpty());
        verify(ingredientRepository).findAllByUserIdAndStatusAndNameContaining(1L, IngredientStatus.ACTIVE, "토마토");
        verify(ingredientRepository, never()).findAllByUserIdAndStatus(anyLong(), any());
    }

    @Test
    void list_withNullName_callsFullList() {
        when(ingredientRepository.findAllByUserIdAndStatus(1L, IngredientStatus.ACTIVE))
                .thenReturn(List.of());

        List<IngredientDto.SummaryResponse> results =
                listIngredientsUseCase.list(new ListIngredientsUseCase.Query(1L, null));

        assertTrue(results.isEmpty());
        verify(ingredientRepository).findAllByUserIdAndStatus(1L, IngredientStatus.ACTIVE);
        verify(ingredientRepository, never()).findAllByUserIdAndStatusAndNameContaining(anyLong(), any(), anyString());
    }

    @Test
    void list_withBlankName_callsFullList() {
        when(ingredientRepository.findAllByUserIdAndStatus(1L, IngredientStatus.ACTIVE))
                .thenReturn(List.of());

        List<IngredientDto.SummaryResponse> results =
                listIngredientsUseCase.list(new ListIngredientsUseCase.Query(1L, "   "));

        assertTrue(results.isEmpty());
        verify(ingredientRepository).findAllByUserIdAndStatus(1L, IngredientStatus.ACTIVE);
        verify(ingredientRepository, never()).findAllByUserIdAndStatusAndNameContaining(anyLong(), any(), anyString());
    }

    @Test
    void list_withNameHavingWhitespace_trimsBeforeSearch() {
        when(ingredientRepository.findAllByUserIdAndStatusAndNameContaining(1L, IngredientStatus.ACTIVE, "우유"))
                .thenReturn(List.of());

        listIngredientsUseCase.list(new ListIngredientsUseCase.Query(1L, "  우유  "));

        verify(ingredientRepository).findAllByUserIdAndStatusAndNameContaining(1L, IngredientStatus.ACTIVE, "우유");
    }

    @Test
    void list_withoutName_usesBackwardCompatibleConstructor() {
        when(ingredientRepository.findAllByUserIdAndStatus(1L, IngredientStatus.ACTIVE))
                .thenReturn(List.of());

        listIngredientsUseCase.list(new ListIngredientsUseCase.Query(1L));

        verify(ingredientRepository).findAllByUserIdAndStatus(1L, IngredientStatus.ACTIVE);
    }
}
