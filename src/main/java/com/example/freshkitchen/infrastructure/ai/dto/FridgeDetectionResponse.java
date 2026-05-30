package com.example.freshkitchen.infrastructure.ai.dto;

import java.util.List;

public record FridgeDetectionResponse(
        List<DetectedItem> items
) {

    public record DetectedItem(
            String name,
            String category
    ) {
    }
}
