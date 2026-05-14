package com.example.freshkitchen.application.image.usecase;

import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.domain.image.enums.StorageProvider;

import java.time.OffsetDateTime;

public interface StoreMultipartImageAssetUseCase {

    Result store(Command command);

    record Command(
            Long userId,
            ImageKind kind,
            String originalFilename,
            String contentType,
            byte[] content
    ) {
    }

    record Result(
            Long imageAssetId,
            ImageKind kind,
            StorageProvider storageProvider,
            String imageUrl,
            OffsetDateTime createdAt
    ) {
    }
}
