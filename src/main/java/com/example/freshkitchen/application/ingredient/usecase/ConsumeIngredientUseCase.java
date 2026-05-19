package com.example.freshkitchen.application.ingredient.usecase;

import java.time.LocalDate;

public interface ConsumeIngredientUseCase {

    ConsumeResult consume(Command command);

    record Command(
            Long ingredientId,
            Long userId
    ) {
    }

    record ConsumeResult(
            LocalDate consumedAt
    ) {
    }
}
