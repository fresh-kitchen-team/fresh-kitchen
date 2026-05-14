package com.example.freshkitchen.application.image.port;

import java.time.OffsetDateTime;

public interface ImageStoragePort {

    UploadUrl createUploadUrl(Command command);

    record Command(
            String objectKey,
            String contentType
    ) {
    }

    record UploadUrl(
            String objectKey,
            String uploadUrl,
            OffsetDateTime expiresAt,
            String contentType
    ) {
    }
}
