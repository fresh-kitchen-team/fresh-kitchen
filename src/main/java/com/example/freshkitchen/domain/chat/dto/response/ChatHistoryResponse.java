package com.example.freshkitchen.domain.chat.dto.response;


// ChatHistoryResponse.java

import lombok.*;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatHistoryResponse {
    private String title;
    private List<MessageResponse> messages;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MessageResponse {
        private Long messageId;
        private String sender;  // "user" | "ai"
        private String text;
        private ChatMessageResponse.AiPayloadResponse aiPayload;
        private String createdAt;
    }
}