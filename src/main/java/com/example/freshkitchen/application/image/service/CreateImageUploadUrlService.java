package com.example.freshkitchen.application.image.service;

import com.example.freshkitchen.application.image.port.ImageStoragePort;
import com.example.freshkitchen.application.image.usecase.CreateImageUploadUrlUseCase;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CreateImageUploadUrlService implements CreateImageUploadUrlUseCase {

    private static final String OBJECT_KEY_PREFIX = "images";

    private final ImageStoragePort imageStoragePort;

    @Override
    public Result create(Command command) {
        validate(command);
        String objectKey = createObjectKey(command.userId(), command.kind(), command.originalFileName());
        ImageStoragePort.UploadUrl uploadUrl = imageStoragePort.createUploadUrl(new ImageStoragePort.Command(
                objectKey,
                command.contentType().trim()
        ));
        return new Result(
                uploadUrl.objectKey(),
                uploadUrl.uploadUrl(),
                uploadUrl.expiresAt(),
                uploadUrl.contentType()
        );
    }

    private static void validate(Command command) {
        if (command == null) {
            throw new BusinessValidationException("command must not be null");
        }
        if (command.userId() == null) {
            throw new BusinessValidationException("userId must not be null");
        }
        if (command.kind() == null) {
            throw new BusinessValidationException("kind must not be null");
        }
        if (isBlank(command.contentType())) {
            throw new BusinessValidationException("contentType must not be blank");
        }
        if (isBlank(command.originalFileName())) {
            throw new BusinessValidationException("originalFileName must not be blank");
        }
    }

    private static String createObjectKey(Long userId, ImageKind kind, String originalFileName) {
        return "%s/%d/%s/%s%s".formatted(
                OBJECT_KEY_PREFIX,
                userId,
                kind.name().toLowerCase(Locale.ROOT),
                UUID.randomUUID(),
                extension(originalFileName)
        );
    }

    private static String extension(String originalFileName) {
        String fileName = originalFileName.trim();
        int extensionStart = fileName.lastIndexOf('.');
        if (extensionStart < 0 || extensionStart == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(extensionStart).toLowerCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
