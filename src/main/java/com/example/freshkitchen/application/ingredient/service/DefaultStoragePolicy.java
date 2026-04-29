package com.example.freshkitchen.application.ingredient.service;

import com.example.freshkitchen.domain.ingredient.entity.Storage;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;

import java.util.Comparator;
import java.util.List;

final class DefaultStoragePolicy {

    private static final List<StorageType> ORDERED_TYPES = List.of(
            StorageType.FRIDGE,
            StorageType.FREEZER,
            StorageType.PANTRY
    );

    private DefaultStoragePolicy() {
    }

    static List<StorageType> orderedTypes() {
        return ORDERED_TYPES;
    }

    static String resolveName(StorageType storageType) {
        return switch (storageType) {
            case FRIDGE -> "Fridge";
            case FREEZER -> "Freezer";
            case PANTRY -> "Pantry";
        };
    }

    static Comparator<Storage> comparator() {
        return Comparator.comparingInt(storage -> ORDERED_TYPES.indexOf(storage.getStorageType()));
    }
}
