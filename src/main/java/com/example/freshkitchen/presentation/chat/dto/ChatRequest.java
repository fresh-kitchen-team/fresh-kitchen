package com.example.freshkitchen.presentation.chat.dto;

import jakarta.validation.constraints.NotBlank;

public class ChatRequest {

    public record SendMessage(
            @NotBlank String message
    ) {
    }

    public record CreateRoom(
            String title
    ) {
    }

    public record UpdateRoomTitle(
            @NotBlank String title
    ) {
    }

    private ChatRequest() {
    }
}
