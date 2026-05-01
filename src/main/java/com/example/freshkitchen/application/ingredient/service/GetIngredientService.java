package com.example.freshkitchen.application.ingredient.service;

import com.example.freshkitchen.application.ingredient.dto.IngredientDto;
import com.example.freshkitchen.application.ingredient.usecase.GetIngredientUseCase;
import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import com.example.freshkitchen.domain.ingredient.repository.IngredientRepositoryImpl;
import com.example.freshkitchen.global.exception.BusinessException;
import com.example.freshkitchen.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetIngredientService implements GetIngredientUseCase {

    private final IngredientRepositoryImpl ingredientRepository;

    @Override
    public IngredientDto.DetailResponse get(Query query) {
        Ingredient ingredient = ingredientRepository.findDetailByIdAndUserId(query.ingredientId(), query.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INGREDIENT_NOT_FOUND));
        return IngredientDto.DetailResponse.from(ingredient);
    }
}
