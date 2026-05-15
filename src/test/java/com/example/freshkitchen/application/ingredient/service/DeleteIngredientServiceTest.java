package com.example.freshkitchen.application.ingredient.service;

import com.example.freshkitchen.application.ingredient.usecase.DeleteIngredientUseCase;
import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import com.example.freshkitchen.domain.ingredient.entity.Storage;
import com.example.freshkitchen.domain.ingredient.enums.ExpirySourceType;
import com.example.freshkitchen.domain.ingredient.enums.IngredientSourceType;
import com.example.freshkitchen.domain.ingredient.enums.IngredientStatus;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;
import com.example.freshkitchen.domain.ingredient.exception.IngredientErrorCode;
import com.example.freshkitchen.domain.ingredient.exception.IngredientException;
import com.example.freshkitchen.domain.ingredient.repository.IngredientRepository;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.enums.Provider;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeleteIngredientServiceTest {

    private final IngredientRepository ingredientRepository = mock(IngredientRepository.class);
    private final Clock clock = Clock.fixed(
            LocalDate.of(2026, 5, 14).atStartOfDay().toInstant(ZoneOffset.UTC),
            ZoneOffset.UTC
    );
    private final DeleteIngredientUseCase deleteIngredientUseCase =
            new DeleteIngredientService(ingredientRepository, clock);

    @Test
    void delete_marksIngredientDiscarded() {
        User user = User.create(new User.CreateCommand("user-1", Provider.GOOGLE));
        Storage storage = Storage.create(new Storage.CreateCommand(user, StorageType.FRIDGE, "Fridge"));
        Ingredient ingredient = Ingredient.create(new Ingredient.CreateCommand(
                user,
                storage,
                null,
                "Tomato",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 20),
                ExpirySourceType.MANUAL,
                null,
                IngredientSourceType.MANUAL
        ));
        when(ingredientRepository.findByIdAndUserIdAndStatus(10L, 1L, IngredientStatus.ACTIVE))
                .thenReturn(Optional.of(ingredient));

        deleteIngredientUseCase.delete(new DeleteIngredientUseCase.Command(10L, 1L));

        assertEquals(IngredientStatus.DISCARDED, ingredient.getStatus());
        assertEquals(LocalDate.of(2026, 5, 14), ingredient.getDiscardedAt());
        assertNull(ingredient.getConsumedAt());
        verify(ingredientRepository).findByIdAndUserIdAndStatus(10L, 1L, IngredientStatus.ACTIVE);
    }

    @Test
    void delete_rejectsNonActiveIngredient() {
        when(ingredientRepository.findByIdAndUserIdAndStatus(10L, 1L, IngredientStatus.ACTIVE))
                .thenReturn(Optional.empty());

        IngredientException exception = assertThrows(
                IngredientException.class,
                () -> deleteIngredientUseCase.delete(new DeleteIngredientUseCase.Command(10L, 1L))
        );

        assertEquals(IngredientErrorCode.INGREDIENT_NOT_FOUND, exception.getErrorCode());
        verify(ingredientRepository).findByIdAndUserIdAndStatus(10L, 1L, IngredientStatus.ACTIVE);
    }
}
