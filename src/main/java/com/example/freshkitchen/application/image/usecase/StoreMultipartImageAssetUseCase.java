package com.example.freshkitchen.application.image.usecase;

import com.example.freshkitchen.domain.image.enums.ImageKind;

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
            String imageUrl
    ) {
    }
}
