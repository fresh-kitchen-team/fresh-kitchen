package com.example.freshkitchen.presentation.image.dto;

public final class ImageResponse {

    private ImageResponse() {
    }

    public record AttachIngredientImage(
            Long ingredientImageId
    ) {
    }
}
