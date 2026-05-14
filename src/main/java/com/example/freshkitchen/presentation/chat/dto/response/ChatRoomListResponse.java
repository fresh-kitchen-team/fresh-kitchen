package com.example.freshkitchen.presentation.chat.dto.response;

import java.util.List;

public record ChatRoomListResponse(
        List<ChatMessageGroup> today,
        List<ChatMessageGroup> last7Days,
        List<ChatMessageGroup> last30Days
) {
    public record ChatMessageGroup(
            Long roomId,
            String title,
            String updatedAt,
            String sender,
            String content
    ) {}
}