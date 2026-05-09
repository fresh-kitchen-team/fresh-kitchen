package com.example.freshkitchen.domain.chat.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageResponse {
    private String title;
    private AiMessageResponse aiMessage;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AiMessageResponse {
        private Long messageId;
        private String sender;
        private String text;
        private AiPayloadResponse aiPayload;
        private String createdAt;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AiPayloadResponse {
        private List<RecipeResponse> recipes;
        private List<String> tips;
        private List<String> missingIngredients;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecipeResponse {
        private String name;
        private List<String> ingredients;
        private List<String> steps;
        private String time;
    }
}