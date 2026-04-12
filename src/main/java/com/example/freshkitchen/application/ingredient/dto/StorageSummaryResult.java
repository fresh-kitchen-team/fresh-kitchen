package com.example.freshkitchen.application.ingredient.dto;

import com.example.freshkitchen.domain.ingredient.entity.Storage;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;

public record StorageSummaryResult(
        Long storageId,
        StorageType storageType,
        String name
) {

    public static StorageSummaryResult from(Storage storage) {
        return new StorageSummaryResult(
                storage.getId(),
                storage.getStorageType(),
                storage.getName()
        );
    }
}
