package com.example.freshkitchen.domain.chat.dto.request;

import jakarta.validation.constraints.Size;

public record CreateChatRoomRequest(@Size(max = 100) String title) {}