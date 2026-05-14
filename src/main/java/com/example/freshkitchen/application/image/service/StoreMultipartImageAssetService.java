package com.example.freshkitchen.application.image.service;

import com.example.freshkitchen.application.image.port.MultipartImageStoragePort;
import com.example.freshkitchen.application.image.usecase.StoreMultipartImageAssetUseCase;
import com.example.freshkitchen.domain.image.entity.ImageAsset;
import com.example.freshkitchen.domain.image.enums.AssetType;
import com.example.freshkitchen.domain.image.enums.ImageContentType;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.domain.image.enums.StorageProvider;
import com.example.freshkitchen.domain.image.repository.ImageAssetRepository;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

@Service
@RequiredArgsConstructor
public class StoreMultipartImageAssetService implements StoreMultipartImageAssetUseCase {

    private final MultipartImageStoragePort multipartImageStoragePort;
    private final ImageAssetRepository imageAssetRepository;
    private final EntityManager entityManager;
    private final TransactionOperations transactionOperations;

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

        try {
            return transactionOperations.execute(status -> saveImageAsset(command, storedImage));
        } catch (RuntimeException e) {
            deleteStoredImage(storedImage, e);
            throw e;
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
