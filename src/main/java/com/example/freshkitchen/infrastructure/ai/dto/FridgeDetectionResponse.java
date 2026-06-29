package com.example.freshkitchen.infrastructure.ai.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

public record FridgeDetectionResponse(
        @JsonAlias({"objects", "detectedItems", "detected_items"})
        List<DetectedItem> items
) {

    public record DetectedItem(
            String name,
            String category
    ) {
    }
}
