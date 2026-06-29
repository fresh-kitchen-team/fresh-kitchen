package com.example.freshkitchen.presentation.chat.dto.response;

import java.time.OffsetDateTime;

public record ChatRoomResponse(
        Long roomId,
        String title,
        OffsetDateTime createdAt
) {}