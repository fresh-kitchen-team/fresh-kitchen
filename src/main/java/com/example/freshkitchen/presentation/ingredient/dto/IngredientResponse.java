package com.example.freshkitchen.presentation.ingredient.dto;

public final class IngredientResponse {

    private IngredientResponse() {
    }

    public record Create(
            Long ingredientId
    ) {
    }
}
