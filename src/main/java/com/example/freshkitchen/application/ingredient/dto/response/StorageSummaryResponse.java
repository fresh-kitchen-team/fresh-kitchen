package com.example.freshkitchen.application.ingredient.dto.response;

import com.example.freshkitchen.domain.ingredient.entity.Storage;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;

public record StorageSummaryResponse(
        Long storageId,
        StorageType storageType,
        String name
) {

    public static StorageSummaryResponse from(Storage storage) {
        return new StorageSummaryResponse(
                storage.getId(),
                storage.getStorageType(),
                storage.getName()
        );
    }
}
