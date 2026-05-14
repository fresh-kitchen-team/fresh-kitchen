package com.example.freshkitchen.infrastructure.image;

import com.example.freshkitchen.application.image.port.MultipartImageStoragePort;
import com.example.freshkitchen.domain.image.enums.ImageContentType;
import com.example.freshkitchen.domain.image.enums.StorageProvider;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "image.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalMultipartImageStorageAdapter implements MultipartImageStoragePort {

    private final LocalImageStorageProperties properties;
    private final ImageStorageUrlFactory imageStorageUrlFactory;

    @Override
    public StoredImage store(Command command) {
        validate(command);
        ImageContentType contentType = ImageContentType.from(command.contentType());
        String relativePath = "images/%d/%s/%s%s".formatted(
                command.userId(),
                command.kind().name().toLowerCase(Locale.ROOT),
                UUID.randomUUID(),
                contentType.extension()
        );
        Path root = Path.of(properties.getRootDir()).toAbsolutePath().normalize();
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessValidationException("invalid image path");
        }

        try {
            Files.createDirectories(target.getParent());
            Files.write(target, command.content(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new BusinessValidationException("failed to store image file", e);
        }

        return new StoredImage(
                relativePath,
                StorageProvider.LOCAL,
                imageStorageUrlFactory.create(StorageProvider.LOCAL, relativePath)
        );
    }

    @Override
    public void delete(DeleteCommand command) {
        validate(command);
        Path root = Path.of(properties.getRootDir()).toAbsolutePath().normalize();
        Path target = root.resolve(command.objectKey()).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessValidationException("invalid image path");
        }

        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new BusinessValidationException("failed to delete image file", e);
        }
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

    private static void validate(DeleteCommand command) {
        if (command == null) {
            throw new BusinessValidationException("command must not be null");
        }
        if (command.objectKey() == null || command.objectKey().isBlank()) {
            throw new BusinessValidationException("objectKey must not be blank");
        }
    }
}
