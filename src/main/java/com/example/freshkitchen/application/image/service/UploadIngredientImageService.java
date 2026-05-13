package com.example.freshkitchen.application.image.service;

import com.example.freshkitchen.application.image.usecase.AttachIngredientImageUseCase;
import com.example.freshkitchen.application.image.usecase.StoreMultipartImageAssetUseCase;
import com.example.freshkitchen.application.image.usecase.UploadIngredientImageUseCase;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.domain.image.enums.IngredientImageSourceType;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UploadIngredientImageService implements UploadIngredientImageUseCase {

    private final StoreMultipartImageAssetUseCase storeMultipartImageAssetUseCase;
    private final AttachIngredientImageUseCase attachIngredientImageUseCase;

    @Override
    public Result upload(Command command) {
        validate(command);
        StoreMultipartImageAssetUseCase.Result storedImage = storeMultipartImageAssetUseCase.store(
                new StoreMultipartImageAssetUseCase.Command(
                        command.userId(),
                        ImageKind.INGREDIENT,
                        command.originalFilename(),
                        command.contentType(),
                        command.content()
                )
        );

        Long ingredientImageId = attachIngredientImageUseCase.attach(new AttachIngredientImageUseCase.Command(
                command.userId(),
                command.ingredientId(),
                storedImage.imageAssetId(),
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
}
