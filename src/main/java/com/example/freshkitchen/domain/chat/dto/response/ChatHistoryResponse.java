package com.example.freshkitchen.domain.chat.dto.response;

import java.util.List;

public record ChatHistoryResponse(
        String title,
        List<MessageResponse> messages
) {
    public record MessageResponse(
            Long messageId,
            String sender,
            String text,
            ChatMessageResponse.AiPayloadResponse aiPayload,
            String createdAt
    ) {}
}