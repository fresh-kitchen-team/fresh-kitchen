package com.example.freshkitchen.application.ingredient.service;

import com.example.freshkitchen.application.image.usecase.AttachIngredientImageUseCase;
import com.example.freshkitchen.application.ingredient.usecase.CreateIngredientUseCase;
import com.example.freshkitchen.application.ingredient.usecase.CreateIngredientWithImageUseCase;
import com.example.freshkitchen.domain.image.enums.IngredientImageSourceType;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateIngredientWithImageService implements CreateIngredientWithImageUseCase {

    private final CreateIngredientUseCase createIngredientUseCase;
    private final AttachIngredientImageUseCase attachIngredientImageUseCase;

    @Override
    public Long create(Command command) {
        validate(command);
        CreateIngredientUseCase.Command ingredientCommand = command.ingredientCommand();
        Long ingredientId = createIngredientUseCase.create(ingredientCommand);

        if (command.imageAssetId() != null) {
            attachIngredientImageUseCase.attach(new AttachIngredientImageUseCase.Command(
                    ingredientCommand.userId(),
                    ingredientId,
                    command.imageAssetId(),
                    true,
                    IngredientImageSourceType.PHOTO
            ));
        }
        return ingredientId;
    }

    private static void validate(Command command) {
        if (command == null) {
            throw new BusinessValidationException("command must not be null");
        }
        if (command.ingredientCommand() == null) {
            throw new BusinessValidationException("ingredientCommand must not be null");
        }
    }
}
