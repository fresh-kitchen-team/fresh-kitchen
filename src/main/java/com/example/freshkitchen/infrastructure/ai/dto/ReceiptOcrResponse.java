package com.example.freshkitchen.infrastructure.ai.dto;

import java.util.List;

public record ReceiptOcrResponse(
        List<String> ingredients
) {
}
