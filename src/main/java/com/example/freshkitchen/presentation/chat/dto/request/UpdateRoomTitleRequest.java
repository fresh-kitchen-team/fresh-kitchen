package com.example.freshkitchen.presentation.chat.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateRoomTitleRequest(@NotBlank String title) {}