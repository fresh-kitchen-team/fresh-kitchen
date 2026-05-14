package com.example.freshkitchen.application.ingredient.service;

import com.example.freshkitchen.application.ingredient.usecase.DeleteIngredientUseCase;
import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import com.example.freshkitchen.domain.ingredient.exception.IngredientErrorCode;
import com.example.freshkitchen.domain.ingredient.exception.IngredientException;
import com.example.freshkitchen.domain.ingredient.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class DeleteIngredientService implements DeleteIngredientUseCase {

    private final IngredientRepository ingredientRepository;
    private final Clock clock;

    @Override
    public void delete(Command command) {
        Ingredient ingredient = ingredientRepository.findByIdAndUserId(command.ingredientId(), command.userId())
                .orElseThrow(() -> new IngredientException(IngredientErrorCode.INGREDIENT_NOT_FOUND));
        ingredient.markDiscarded(LocalDate.now(clock));
    }
}
