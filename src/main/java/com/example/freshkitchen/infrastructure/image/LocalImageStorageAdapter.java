package com.example.freshkitchen.infrastructure.image;

import com.example.freshkitchen.application.image.port.ImageStoragePort;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class LocalImageStorageAdapter implements ImageStoragePort {

    private static final String LOCAL_UPLOAD_BASE_URL = "https://local-storage.freshkitchen.test/upload/";
    private static final long UPLOAD_URL_EXPIRATION_MINUTES = 10L;

    @Override
    public UploadUrl createUploadUrl(Command command) {
        validate(command);
        return new UploadUrl(
                command.objectKey(),
                LOCAL_UPLOAD_BASE_URL + command.objectKey(),
                OffsetDateTime.now().plusMinutes(UPLOAD_URL_EXPIRATION_MINUTES),
                command.contentType()
        );
    }

    private static void validate(Command command) {
        if (command == null) {
            throw new BusinessValidationException("command must not be null");
        }
        if (command.objectKey() == null || command.objectKey().isBlank()) {
            throw new BusinessValidationException("objectKey must not be blank");
        }
        if (command.contentType() == null || command.contentType().isBlank()) {
            throw new BusinessValidationException("contentType must not be blank");
        }
    }
}
