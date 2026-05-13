package com.example.freshkitchen.application.chat.dto;

import com.example.freshkitchen.domain.chat.entity.ChatRoom;

import java.time.OffsetDateTime;

public record ChatRoomResult(
        Long id,
        String title,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static ChatRoomResult from(ChatRoom room) {
        return new ChatRoomResult(
                room.getId(),
                room.getTitle(),
                room.getCreatedAt(),
                room.getUpdatedAt()
        );
    }
}
