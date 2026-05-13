package com.example.freshkitchen.application.image.service;

import com.example.freshkitchen.application.image.port.MultipartImageStoragePort;
import com.example.freshkitchen.application.image.usecase.StoreMultipartImageAssetUseCase;
import com.example.freshkitchen.domain.image.entity.ImageAsset;
import com.example.freshkitchen.domain.image.enums.AssetType;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.domain.image.enums.StorageProvider;
import com.example.freshkitchen.domain.image.repository.ImageAssetRepository;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class StoreMultipartImageAssetService implements StoreMultipartImageAssetUseCase {

    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final MultipartImageStoragePort multipartImageStoragePort;
    private final ImageAssetRepository imageAssetRepository;
    private final EntityManager entityManager;

    @Override
    public Result store(Command command) {
        validate(command);
        String contentType = normalizeContentType(command.contentType());
        MultipartImageStoragePort.StoredImage storedImage = multipartImageStoragePort.store(
                new MultipartImageStoragePort.Command(
                        command.userId(),
                        command.kind(),
                        command.originalFilename(),
                        contentType,
                        command.content()
                )
        );

        User user = entityManager.getReference(User.class, command.userId());
        ImageAsset imageAsset = ImageAsset.create(new ImageAsset.CreateCommand(
                user,
                AssetType.USER_UPLOAD,
                command.kind(),
                StorageProvider.LOCAL,
                storedImage.objectKey(),
                null,
                null
        ));
        ImageAsset savedImageAsset = imageAssetRepository.save(imageAsset);
        return new Result(savedImageAsset.getId(), savedImageAsset.getObjectKey());
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

    private static String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new BusinessValidationException("contentType must not be blank");
        }
        String normalized = contentType.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_CONTENT_TYPES.contains(normalized)) {
            throw new BusinessValidationException("contentType must be supported image type");
        }
        return normalized;
    }
}
