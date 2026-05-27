package com.example.freshkitchen.presentation.chat.dto.response;

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
            String uiType,
            String createdAt
    ) {}

    public record AiPayloadResponse(
            List<RecipeResponse> recipes,
            List<String> tips,
            List<String> missingIngredients,
            List<MatchedItemResponse> matchedItems
    ) {}

    public record RecipeResponse(
            String name,
            List<String> ingredients,
            List<String> steps,
            String time
    ) {}

    public record MatchedItemResponse(
            Long itemId,
            String name
    ) {}


}
