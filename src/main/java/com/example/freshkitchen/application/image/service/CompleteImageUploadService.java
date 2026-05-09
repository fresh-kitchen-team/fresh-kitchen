package com.example.freshkitchen.application.image.service;

import com.example.freshkitchen.application.image.usecase.CompleteImageUploadUseCase;
import com.example.freshkitchen.domain.image.entity.ImageAsset;
import com.example.freshkitchen.domain.image.enums.AssetType;
import com.example.freshkitchen.domain.image.enums.StorageProvider;
import com.example.freshkitchen.domain.image.repository.ImageAssetRepository;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class CompleteImageUploadService implements CompleteImageUploadUseCase {

    private static final String OBJECT_KEY_FORMAT = "images/%d/%s/";
    private static final Pattern OBJECT_FILE_NAME_PATTERN = Pattern.compile(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)"
    );

    private final ImageAssetRepository imageAssetRepository;
    private final EntityManager entityManager;

    @Override
    public Long complete(Command command) {
        validate(command);
        User user = entityManager.getReference(User.class, command.userId());
        ImageAsset imageAsset = ImageAsset.create(new ImageAsset.CreateCommand(
                user,
                AssetType.USER_UPLOAD,
                command.kind(),
                StorageProvider.S3,
                command.objectKey(),
                command.width(),
                command.height()
        ));
        return imageAssetRepository.save(imageAsset).getId();
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
        if (command.objectKey() == null || command.objectKey().isBlank()) {
            throw new BusinessValidationException("objectKey must not be blank");
        }
        validateObjectKey(command);
    }

    private static void validateObjectKey(Command command) {
        String expectedPrefix = OBJECT_KEY_FORMAT.formatted(
                command.userId(),
                command.kind().name().toLowerCase(Locale.ROOT)
        );
        if (!command.objectKey().startsWith(expectedPrefix)) {
            throw new BusinessValidationException("objectKey must match upload path");
        }

        String objectFileName = command.objectKey().substring(expectedPrefix.length());
        if (!OBJECT_FILE_NAME_PATTERN.matcher(objectFileName).matches()) {
            throw new BusinessValidationException("objectKey must match upload path");
        }
    }
}
