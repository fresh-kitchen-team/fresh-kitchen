package com.example.freshkitchen.application.ingredient.service;

import com.example.freshkitchen.application.ingredient.dto.IngredientDto;
import com.example.freshkitchen.application.ingredient.usecase.GetIngredientUseCase;
import com.example.freshkitchen.application.image.port.ImageAssetUrlResolver;
import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import com.example.freshkitchen.domain.ingredient.exception.IngredientErrorCode;
import com.example.freshkitchen.domain.ingredient.exception.IngredientException;
import com.example.freshkitchen.domain.ingredient.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetIngredientService implements GetIngredientUseCase {

    private final IngredientRepository ingredientRepository;
    private final ImageAssetUrlResolver imageAssetUrlResolver;

    @Override
    public IngredientDto.DetailResponse get(Query query) {
        Ingredient ingredient = ingredientRepository.findDetailByIdAndUserId(query.ingredientId(), query.userId())
                .orElseThrow(() -> new IngredientException(IngredientErrorCode.INGREDIENT_NOT_FOUND));
        return IngredientDto.DetailResponse.from(ingredient, imageAssetUrlResolver);
    }
}
