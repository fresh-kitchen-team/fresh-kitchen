package com.example.freshkitchen.domain.chat.dto.response;

import java.time.OffsetDateTime;

public record ChatRoomResponse(
        Long roomId,
        String title,
        OffsetDateTime createdAt
) {}