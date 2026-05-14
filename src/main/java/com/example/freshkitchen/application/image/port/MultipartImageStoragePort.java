package com.example.freshkitchen.application.image.port;

import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.domain.image.enums.StorageProvider;

public interface MultipartImageStoragePort {

    StoredImage store(Command command);

    void delete(DeleteCommand command);

    record Command(
            Long userId,
            ImageKind kind,
            String originalFilename,
            String contentType,
            byte[] content
    ) {
    }

    record StoredImage(
            String objectKey,
            StorageProvider storageProvider,
            String imageUrl
    ) {
    }

    record DeleteCommand(
            String objectKey
    ) {
    }
}
