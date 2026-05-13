package com.example.freshkitchen.infrastructure.image;

import com.example.freshkitchen.application.image.port.MultipartImageStoragePort;
import com.example.freshkitchen.domain.image.enums.StorageProvider;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.Locale;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "image.storage.type", havingValue = "s3")
public class S3MultipartImageStorageAdapter implements MultipartImageStoragePort {

    private final S3Client s3Client;
    private final S3ImageStorageProperties properties;

    @Override
    public StoredImage store(Command command) {
        String contentType = validateAndNormalizeContentType(command);
        String objectKey = "images/%d/%s/%s%s".formatted(
                command.userId(),
                command.kind().name().toLowerCase(Locale.ROOT),
                UUID.randomUUID(),
                extension(contentType)
        );

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(objectKey)
                .contentType(contentType)
                .build();
        s3Client.putObject(request, RequestBody.fromBytes(command.content()));

        return new StoredImage(objectKey, StorageProvider.S3, imageUrl(objectKey));
    }

    private String imageUrl(String objectKey) {
        String baseUrl = properties.getPublicBaseUrl();
        if (baseUrl != null && !baseUrl.isBlank()) {
            String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            return normalized + "/" + objectKey;
        }
        return "https://%s.s3.%s.amazonaws.com/%s".formatted(
                properties.getBucket(),
                properties.getRegion(),
                objectKey
        );
    }

    private static String validateAndNormalizeContentType(Command command) {
        if (command == null) {
            throw new BusinessValidationException("command must not be null");
        }
        if (command.userId() == null) {
            throw new BusinessValidationException("userId must not be null");
        }
        if (command.kind() == null) {
            throw new BusinessValidationException("kind must not be null");
        }
        if (command.content() == null || command.content().length == 0) {
            throw new BusinessValidationException("file must not be empty");
        }
        return normalizeContentType(command.contentType());
    }

    private static String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new BusinessValidationException("contentType must not be blank");
        }
        String normalized = contentType.trim().toLowerCase(Locale.ROOT);
        if (!isSupportedContentType(normalized)) {
            throw new BusinessValidationException("contentType must be supported image type");
        }
        return normalized;
    }

    private static boolean isSupportedContentType(String contentType) {
        return switch (contentType) {
            case "image/jpeg", "image/png", "image/webp" -> true;
            default -> false;
        };
    }

    private static String extension(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new BusinessValidationException("contentType must be supported image type");
        };
    }
}
