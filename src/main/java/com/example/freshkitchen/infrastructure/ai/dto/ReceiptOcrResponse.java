package com.example.freshkitchen.infrastructure.ai.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.List;

public record ReceiptOcrResponse(
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        LocalDate purchasedAt,
        List<String> ingredients
) {
}
