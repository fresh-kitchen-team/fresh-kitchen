package com.example.freshkitchen.application.image.port;

import com.example.freshkitchen.domain.image.enums.ImageKind;

public interface MultipartImageStoragePort {

    StoredImage store(Command command);

    record Command(
            Long userId,
            ImageKind kind,
            String originalFilename,
            String contentType,
            byte[] content
    ) {
    }

    record StoredImage(
            String objectKey
    ) {
    }
}
