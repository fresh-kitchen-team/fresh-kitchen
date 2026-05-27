package com.example.freshkitchen.presentation.chat.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ConsumeItemsRequest(
        @NotEmpty List<Long> itemIds
) {
}