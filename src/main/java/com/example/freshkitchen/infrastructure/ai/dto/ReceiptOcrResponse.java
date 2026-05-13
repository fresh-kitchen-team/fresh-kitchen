package com.example.freshkitchen.infrastructure.ai.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.example.freshkitchen.domain.ingredient.enums.ExpirySourceType;

import java.time.LocalDate;
import java.util.List;

public record ReceiptOcrResponse(
        String storeName,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        LocalDate purchasedAt,
        List<RecognizedItem> recognizedItems,
        String ocrText
) {
    public record RecognizedItem(
            String name,
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            LocalDate estimatedExpiresAt,
            ExpirySourceType expirySourceType,
            Double confidence
    ) {
    }
}
