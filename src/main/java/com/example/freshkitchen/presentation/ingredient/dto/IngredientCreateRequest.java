package com.example.freshkitchen.presentation.ingredient.dto;

import com.example.freshkitchen.application.ingredient.usecase.CreateIngredientUseCase;
import com.example.freshkitchen.domain.ingredient.enums.ExpirySourceType;
import com.example.freshkitchen.domain.ingredient.enums.IngredientSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record IngredientCreateRequest(
        @NotNull @Positive Long storageId,
        @Positive Long catalogId,
        @NotBlank @Size(max = 100) String name,
        LocalDate registeredAt,
        LocalDate expiresAt,
        @NotNull ExpirySourceType expirySourceType,
        String note,
        @NotNull IngredientSourceType sourceType
) {

    public CreateIngredientUseCase.Command toCommand(Long userId) {
        return new CreateIngredientUseCase.Command(
                userId,
                storageId,
                catalogId,
                name,
                registeredAt,
                expiresAt,
                expirySourceType,
                note,
                sourceType
        );
    }
}
