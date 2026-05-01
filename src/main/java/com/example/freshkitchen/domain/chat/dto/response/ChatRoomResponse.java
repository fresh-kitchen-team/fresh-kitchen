package com.example.freshkitchen.domain.chat.dto.response;


import lombok.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomResponse {
    private Long roomId;
    private String title;
    private OffsetDateTime createdAt;
}