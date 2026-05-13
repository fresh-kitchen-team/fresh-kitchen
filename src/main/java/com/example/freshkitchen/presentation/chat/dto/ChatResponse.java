package com.example.freshkitchen.presentation.chat.dto;

import com.example.freshkitchen.application.chat.dto.ChatMessageResult;
import com.example.freshkitchen.application.chat.dto.ChatRoomResult;
import com.example.freshkitchen.domain.chat.enums.Sender;

import java.time.OffsetDateTime;

public class ChatResponse {

    public record Room(
            Long id,
            String title,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        public static Room from(ChatRoomResult result) {
            return new Room(result.id(), result.title(), result.createdAt(), result.updatedAt());
        }
    }

    public record Message(
            Long id,
            String content,
            Sender sender,
            String aiPayload,
            OffsetDateTime createdAt
    ) {
        public static Message from(ChatMessageResult result) {
            return new Message(
                    result.id(), result.content(), result.sender(),
                    result.aiPayload(), result.createdAt()
            );
        }
    }

    private ChatResponse() {
    }
}
