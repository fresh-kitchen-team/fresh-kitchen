package com.example.freshkitchen.presentation.chat.dto.request;

import jakarta.validation.constraints.Size;

public record CreateChatRoomRequest(@Size(max = 100) String title) {}