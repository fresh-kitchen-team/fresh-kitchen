package com.example.freshkitchen.presentation.image.dto;

import jakarta.validation.constraints.NotNull;

public final class ImageRequest {

    private ImageRequest() {
    }

    public record ChangePrimary(
            @NotNull Long ingredientImageId
    ) {
    }
}
