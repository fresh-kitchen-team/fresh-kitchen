package com.example.freshkitchen.application.image.usecase;

import com.example.freshkitchen.domain.image.enums.ImageKind;

import java.time.OffsetDateTime;

public interface CreateImageUploadUrlUseCase {

    Result create(Command command);

    record Command(
            Long userId,
            ImageKind kind,
            String originalFileName,
            String contentType
    ) {
    }

    record Result(
            String objectKey,
            String uploadUrl,
            OffsetDateTime expiresAt,
            String contentType
    ) {
    }
}
