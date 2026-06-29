package com.example.freshkitchen.application.image.usecase;

public interface DeleteStoredImageAssetUseCase {

    void deleteIfUnattached(Command command);

    record Command(
            Long userId,
            Long imageAssetId
    ) {
    }
}
