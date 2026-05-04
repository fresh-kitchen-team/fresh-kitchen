package com.example.freshkitchen.infrastructure.ai.dto;

import java.util.List;

public record FridgeDetectionResponse(
        List<DetectedObject> objects
) {

    public record DetectedObject(
            String name,
            Double confidence,
            Box box
    ) {
    }

    public record Box(
            Double x1,
            Double y1,
            Double x2,
            Double y2
    ) {
    }
}
