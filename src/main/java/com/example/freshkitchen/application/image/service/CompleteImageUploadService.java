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

@Service
@RequiredArgsConstructor
@Transactional
public class CompleteImageUploadService implements CompleteImageUploadUseCase {

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
    }
}
