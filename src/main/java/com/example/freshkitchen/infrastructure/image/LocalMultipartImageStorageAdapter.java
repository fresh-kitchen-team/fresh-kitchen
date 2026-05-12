package com.example.freshkitchen.infrastructure.image;

import com.example.freshkitchen.application.image.port.MultipartImageStoragePort;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LocalMultipartImageStorageAdapter implements MultipartImageStoragePort {

    private final LocalImageStorageProperties properties;

    @Override
    public StoredImage store(Command command) {
        validate(command);
        String relativePath = "images/%d/%s/%s%s".formatted(
                command.userId(),
                command.kind().name().toLowerCase(Locale.ROOT),
                UUID.randomUUID(),
                extension(command.contentType())
        );
        Path root = Path.of(properties.getRootDir()).toAbsolutePath().normalize();
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessValidationException("invalid image path");
        }

        try {
            Files.createDirectories(target.getParent());
            Files.write(target, command.content(), StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new BusinessValidationException("failed to store image file", e);
        }

        return new StoredImage(publicObjectKey(relativePath));
    }

    private String publicObjectKey(String relativePath) {
        String baseUrl = properties.getPublicBaseUrl();
        if (baseUrl.endsWith("/")) {
            return baseUrl + relativePath;
        }
        return baseUrl + "/" + relativePath;
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

    private static String extension(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new BusinessValidationException("contentType must be supported image type");
        };
    }
}
