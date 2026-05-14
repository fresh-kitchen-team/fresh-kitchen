package com.example.freshkitchen.application.image.service;

import com.example.freshkitchen.application.image.port.MultipartImageStoragePort;
import com.example.freshkitchen.application.image.usecase.DeleteStoredImageAssetUseCase;
import com.example.freshkitchen.domain.image.entity.ImageAsset;
import com.example.freshkitchen.domain.image.repository.ImageAssetRepository;
import com.example.freshkitchen.domain.image.repository.IngredientImageRepository;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

@Service
@RequiredArgsConstructor
public class DeleteStoredImageAssetService implements DeleteStoredImageAssetUseCase {

    private final ImageAssetRepository imageAssetRepository;
    private final IngredientImageRepository ingredientImageRepository;
    private final MultipartImageStoragePort multipartImageStoragePort;
    private final TransactionOperations transactionOperations;

    @Override
    public void deleteIfUnattached(Command command) {
        validate(command);
        DeletedImage deletedImage = transactionOperations.execute(status -> deleteImageAsset(command));
        if (deletedImage != null) {
            multipartImageStoragePort.delete(new MultipartImageStoragePort.DeleteCommand(deletedImage.objectKey()));
        }
    }

    private DeletedImage deleteImageAsset(Command command) {
        ImageAsset imageAsset = imageAssetRepository.findByIdAndUserId(command.imageAssetId(), command.userId())
                .orElse(null);
        if (imageAsset == null || ingredientImageRepository.existsByImageAssetId(imageAsset.getId())) {
            return null;
        }

        DeletedImage deletedImage = new DeletedImage(imageAsset.getObjectKey());
        imageAssetRepository.delete(imageAsset);
        return deletedImage;
    }

    private static void validate(Command command) {
        if (command == null) {
            throw new BusinessValidationException("command must not be null");
        }
        if (command.userId() == null) {
            throw new BusinessValidationException("userId must not be null");
        }
        if (command.imageAssetId() == null) {
            throw new BusinessValidationException("imageAssetId must not be null");
        }
    }

    private record DeletedImage(String objectKey) {
    }
}
