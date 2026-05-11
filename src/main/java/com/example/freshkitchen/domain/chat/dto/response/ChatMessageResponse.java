package com.example.freshkitchen.domain.chat.dto.response;

import java.util.List;

public record ChatMessageResponse(
        String title,
        AiMessageResponse aiMessage
) {
    public record AiMessageResponse(
            Long messageId,
            String sender,
            String text,
            AiPayloadResponse aiPayload,
            String createdAt
    ) {}

    public record AiPayloadResponse(
            List<RecipeResponse> recipes,
            List<String> tips,
            List<String> missingIngredients
    ) {}

    public record RecipeResponse(
            String name,
            List<String> ingredients,
            List<String> steps,
            String time
    ) {}
}