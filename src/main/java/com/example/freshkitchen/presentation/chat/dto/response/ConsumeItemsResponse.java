package com.example.freshkitchen.presentation.chat.dto.response;

import java.time.LocalDate;
import java.util.List;

public record ConsumeItemsResponse(
        LocalDate consumedAt,
        List<Long> consumedItemIds,
        List<SkippedItem> skippedItems
) {
    public record SkippedItem(
            Long itemId,
            String reason
    ) {
    }
}
