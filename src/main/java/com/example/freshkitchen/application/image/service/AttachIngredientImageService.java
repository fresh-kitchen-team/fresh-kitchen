package com.example.freshkitchen.application.image.service;

import com.example.freshkitchen.application.image.usecase.AttachIngredientImageUseCase;
import com.example.freshkitchen.domain.image.entity.ImageAsset;
import com.example.freshkitchen.domain.image.entity.IngredientImage;
import com.example.freshkitchen.domain.image.exception.ImageErrorCode;
import com.example.freshkitchen.domain.image.exception.ImageException;
import com.example.freshkitchen.domain.image.repository.ImageAssetRepository;
import com.example.freshkitchen.domain.image.repository.IngredientImageRepository;
import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import com.example.freshkitchen.domain.ingredient.enums.IngredientStatus;
import com.example.freshkitchen.domain.ingredient.exception.IngredientErrorCode;
import com.example.freshkitchen.domain.ingredient.exception.IngredientException;
import com.example.freshkitchen.domain.ingredient.repository.IngredientRepository;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AttachIngredientImageService implements AttachIngredientImageUseCase {

    private final IngredientRepository ingredientRepository;
    private final ImageAssetRepository imageAssetRepository;
    private final IngredientImageRepository ingredientImageRepository;

    @Override
    public Long attach(Command command) {
        validate(command);
        Ingredient ingredient = findOwnedIngredient(command.ingredientId(), command.userId());
        ImageAsset imageAsset = imageAssetRepository.findAttachableByIdAndUserId(command.imageAssetId(), command.userId())
                .orElseThrow(() -> new ImageException(ImageErrorCode.IMAGE_ASSET_NOT_FOUND));

        if (ingredientImageRepository.existsByIngredientIdAndImageAssetId(command.ingredientId(), command.imageAssetId())) {
            throw new ImageException(ImageErrorCode.IMAGE_ASSET_ALREADY_ATTACHED);
        }

        IngredientImage ingredientImage = IngredientImage.create(new IngredientImage.CreateCommand(
                ingredient,
                imageAsset,
                command.primary(),
                command.sourceType()
        ));
        return ingredientImageRepository.save(ingredientImage).getId();
    }

    private Ingredient findOwnedIngredient(Long ingredientId, Long userId) {
        return ingredientRepository.findByIdAndUserIdAndStatusWithImagesForUpdate(
                        ingredientId,
                        userId,
                        IngredientStatus.ACTIVE
                )
                .orElseThrow(() -> new IngredientException(IngredientErrorCode.INGREDIENT_NOT_FOUND));
    }

    private static void validate(Command command) {
        if (command == null) {
            throw new BusinessValidationException("command must not be null");
        }
        if (command.userId() == null) {
            throw new BusinessValidationException("userId must not be null");
        }
        if (command.ingredientId() == null) {
            throw new IngredientException(IngredientErrorCode.INGREDIENT_ID_REQUIRED);
        }
        if (command.imageAssetId() == null) {
            throw new ImageException(ImageErrorCode.IMAGE_ASSET_ID_REQUIRED);
        }
        if (command.sourceType() == null) {
            throw new BusinessValidationException("sourceType must not be null");
        }
    }
}
