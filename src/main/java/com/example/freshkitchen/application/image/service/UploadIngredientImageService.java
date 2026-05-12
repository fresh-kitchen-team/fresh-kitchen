package com.example.freshkitchen.application.image.service;

import com.example.freshkitchen.application.image.port.MultipartImageStoragePort;
import com.example.freshkitchen.application.image.usecase.AttachIngredientImageUseCase;
import com.example.freshkitchen.application.image.usecase.UploadIngredientImageUseCase;
import com.example.freshkitchen.domain.image.entity.ImageAsset;
import com.example.freshkitchen.domain.image.enums.AssetType;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.domain.image.enums.IngredientImageSourceType;
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
public class UploadIngredientImageService implements UploadIngredientImageUseCase {

    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final MultipartImageStoragePort multipartImageStoragePort;
    private final ImageAssetRepository imageAssetRepository;
    private final AttachIngredientImageUseCase attachIngredientImageUseCase;
    private final EntityManager entityManager;

    @Override
    public Result upload(Command command) {
        validate(command);
        String contentType = normalizeContentType(command.contentType());
        MultipartImageStoragePort.StoredImage storedImage = multipartImageStoragePort.store(
                new MultipartImageStoragePort.Command(
                        command.userId(),
                        ImageKind.INGREDIENT,
                        command.originalFilename(),
                        contentType,
                        command.content()
                )
        );

        User user = entityManager.getReference(User.class, command.userId());
        ImageAsset imageAsset = ImageAsset.create(new ImageAsset.CreateCommand(
                user,
                AssetType.USER_UPLOAD,
                ImageKind.INGREDIENT,
                StorageProvider.LOCAL,
                storedImage.objectKey(),
                null,
                null
        ));
        Long imageAssetId = imageAssetRepository.save(imageAsset).getId();

        Long ingredientImageId = attachIngredientImageUseCase.attach(new AttachIngredientImageUseCase.Command(
                command.userId(),
                command.ingredientId(),
                imageAssetId,
                command.primary(),
                command.sourceType()
        ));
        return new Result(ingredientImageId);
    }

    private static void validate(Command command) {
        if (command == null) {
            throw new BusinessValidationException("command must not be null");
        }
        if (command.userId() == null) {
            throw new BusinessValidationException("userId must not be null");
        }
        if (command.ingredientId() == null) {
            throw new BusinessValidationException("ingredientId must not be null");
        }
        if (command.content() == null || command.content().length == 0) {
            throw new BusinessValidationException("file must not be empty");
        }
        if (command.sourceType() == null) {
            throw new BusinessValidationException("sourceType must not be null");
        }
        if (command.sourceType() == IngredientImageSourceType.DEFAULT) {
            throw new BusinessValidationException("sourceType must be PHOTO for uploaded images");
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
