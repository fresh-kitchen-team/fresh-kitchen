package com.example.freshkitchen.application.image.service;

import com.example.freshkitchen.application.image.port.MultipartImageStoragePort;
import com.example.freshkitchen.application.image.port.ThumbnailImageGenerator;
import com.example.freshkitchen.application.image.usecase.StoreMultipartImageAssetUseCase;
import com.example.freshkitchen.domain.image.entity.ImageAsset;
import com.example.freshkitchen.domain.image.entity.ImageVariant;
import com.example.freshkitchen.domain.image.enums.AssetType;
import com.example.freshkitchen.domain.image.enums.ImageContentType;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.domain.image.enums.ImageVariantType;
import com.example.freshkitchen.domain.image.enums.StorageProvider;
import com.example.freshkitchen.domain.image.repository.ImageAssetRepository;
import com.example.freshkitchen.domain.image.repository.ImageVariantRepository;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreMultipartImageAssetService implements StoreMultipartImageAssetUseCase {

    private final MultipartImageStoragePort multipartImageStoragePort;
    private final ImageAssetRepository imageAssetRepository;
    private final ImageVariantRepository imageVariantRepository;
    private final ThumbnailImageGenerator thumbnailImageGenerator;
    private final EntityManager entityManager;
    private final TransactionOperations transactionOperations;

    @Value("${image.thumbnail.enabled:true}")
    private boolean thumbnailEnabled;

    @Value("${image.thumbnail.max-dimension:320}")
    private int thumbnailMaxDimension;

    @Override
    public Result store(Command command) {
        validate(command);
        String contentType = ImageContentType.from(command.contentType()).value();
        MultipartImageStoragePort.StoredImage storedImage = multipartImageStoragePort.store(
                new MultipartImageStoragePort.Command(
                        command.userId(),
                        command.kind(),
                        command.originalFilename(),
                        contentType,
                        command.content()
                )
        );

        Result result;
        try {
            result = transactionOperations.execute(status -> saveImageAsset(command, storedImage));
        } catch (RuntimeException e) {
            deleteStoredImage(storedImage, e);
            throw e;
        }

        generateThumbnailQuietly(command, result.imageAssetId());
        return result;
    }

    /**
     * 원본 자산 커밋 후 썸네일을 best-effort로 생성한다.
     * 실패해도 원본 업로드는 성공으로 처리하며, 저장된 썸네일 객체가 있으면 정리한다.
     */
    private void generateThumbnailQuietly(Command command, Long imageAssetId) {
        if (!thumbnailEnabled) {
            return;
        }
        Optional<ThumbnailImageGenerator.Thumbnail> generated =
                thumbnailImageGenerator.generate(command.content(), thumbnailMaxDimension);
        if (generated.isEmpty()) {
            return;
        }
        ThumbnailImageGenerator.Thumbnail thumbnail = generated.get();

        MultipartImageStoragePort.StoredImage storedThumbnail;
        try {
            storedThumbnail = multipartImageStoragePort.store(new MultipartImageStoragePort.Command(
                    command.userId(),
                    command.kind(),
                    command.originalFilename(),
                    thumbnail.contentType(),
                    thumbnail.content()
            ));
        } catch (RuntimeException e) {
            log.warn("[Thumbnail] storage failed, skipping variant. imageAssetId={}", imageAssetId, e);
            return;
        }

        try {
            transactionOperations.executeWithoutResult(status -> {
                ImageAsset imageAsset = entityManager.getReference(ImageAsset.class, imageAssetId);
                ImageVariant variant = ImageVariant.create(new ImageVariant.CreateCommand(
                        imageAsset,
                        ImageVariantType.THUMBNAIL,
                        storedThumbnail.objectKey(),
                        thumbnail.width(),
                        thumbnail.height()
                ));
                imageVariantRepository.save(variant);
            });
        } catch (RuntimeException e) {
            log.warn("[Thumbnail] variant persist failed, cleaning up stored object. imageAssetId={}",
                    imageAssetId, e);
            deleteStoredImageQuietly(storedThumbnail);
        }
    }

    private void deleteStoredImageQuietly(MultipartImageStoragePort.StoredImage storedImage) {
        try {
            multipartImageStoragePort.delete(new MultipartImageStoragePort.DeleteCommand(storedImage.objectKey()));
        } catch (RuntimeException cleanupFailure) {
            log.warn("[Thumbnail] cleanup of stored thumbnail failed. objectKey={} reason={}",
                    storedImage.objectKey(), cleanupFailure.getMessage());
        }
    }

    private Result saveImageAsset(Command command, MultipartImageStoragePort.StoredImage storedImage) {
        User user = entityManager.getReference(User.class, command.userId());
        ImageAsset imageAsset = ImageAsset.create(new ImageAsset.CreateCommand(
                user,
                AssetType.USER_UPLOAD,
                command.kind(),
                storedImage.storageProvider(),
                storedImage.objectKey(),
                null,
                null
        ));
        ImageAsset savedImageAsset = imageAssetRepository.save(imageAsset);
        return new Result(
                savedImageAsset.getId(),
                savedImageAsset.getKind(),
                savedImageAsset.getStorageProvider(),
                storedImage.imageUrl(),
                savedImageAsset.getCreatedAt()
        );
    }

    private void deleteStoredImage(MultipartImageStoragePort.StoredImage storedImage, RuntimeException original) {
        try {
            multipartImageStoragePort.delete(new MultipartImageStoragePort.DeleteCommand(storedImage.objectKey()));
        } catch (RuntimeException cleanupFailure) {
            original.addSuppressed(cleanupFailure);
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

}
