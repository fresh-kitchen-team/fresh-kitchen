package com.example.freshkitchen.domain.chat.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateRoomTitleRequest(@NotBlank String title) {}