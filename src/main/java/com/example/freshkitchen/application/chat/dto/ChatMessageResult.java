package com.example.freshkitchen.application.chat.dto;

import com.example.freshkitchen.domain.chat.entity.ChatMessage;
import com.example.freshkitchen.domain.chat.enums.Sender;

import java.time.OffsetDateTime;

public record ChatMessageResult(
        Long id,
        String content,
        Sender sender,
        String aiPayload,
        OffsetDateTime createdAt
) {

    public static ChatMessageResult from(ChatMessage message) {
        return new ChatMessageResult(
                message.getId(),
                message.getContent(),
                message.getSender(),
                message.getAiPayload(),
                message.getCreatedAt()
        );
    }
}
