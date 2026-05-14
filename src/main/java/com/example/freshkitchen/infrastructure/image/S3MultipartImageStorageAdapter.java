package com.example.freshkitchen.infrastructure.image;

import com.example.freshkitchen.application.image.port.MultipartImageStoragePort;
import com.example.freshkitchen.domain.image.enums.ImageContentType;
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
    private final ImageStorageUrlFactory imageStorageUrlFactory;

    @Override
    public StoredImage store(Command command) {
        validate(command);
        ImageContentType contentType = ImageContentType.from(command.contentType());
        String objectKey = "images/%d/%s/%s%s".formatted(
                command.userId(),
                command.kind().name().toLowerCase(Locale.ROOT),
                UUID.randomUUID(),
                contentType.extension()
        );

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(objectKey)
                .contentType(contentType.value())
                .build();
        s3Client.putObject(request, RequestBody.fromBytes(command.content()));

        return new StoredImage(
                objectKey,
                StorageProvider.S3,
                imageStorageUrlFactory.create(StorageProvider.S3, objectKey)
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
        if (command.content() == null || command.content().length == 0) {
            throw new BusinessValidationException("file must not be empty");
        }
    }
}
