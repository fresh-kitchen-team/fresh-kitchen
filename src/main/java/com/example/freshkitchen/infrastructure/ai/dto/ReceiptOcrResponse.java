package com.example.freshkitchen.infrastructure.ai.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.List;

public record ReceiptOcrResponse(
        @JsonAlias("purchased_at")
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        LocalDate purchasedAt,
        @JsonAlias({"ingredient_items", "recognizedItems", "recognized_items"})
        List<IngredientItem> ingredients
) {

    public record IngredientItem(
            String name,
            String category
    ) {
    }
}
