package com.example.freshkitchen.application.image.usecase;

import com.example.freshkitchen.domain.image.enums.ImageKind;

public interface CompleteImageUploadUseCase {

    Long complete(Command command);

    record Command(
            Long userId,
            ImageKind kind,
            String objectKey,
            Integer width,
            Integer height
    ) {
    }
}
