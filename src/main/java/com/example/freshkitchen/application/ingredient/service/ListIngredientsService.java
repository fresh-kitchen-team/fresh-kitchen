package com.example.freshkitchen.application.ingredient.service;

import com.example.freshkitchen.application.ingredient.dto.IngredientDto;
import com.example.freshkitchen.application.ingredient.usecase.ListIngredientsUseCase;
import com.example.freshkitchen.application.image.port.ImageAssetUrlResolver;
import com.example.freshkitchen.domain.ingredient.enums.IngredientStatus;
import com.example.freshkitchen.domain.ingredient.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListIngredientsService implements ListIngredientsUseCase {

    private final IngredientRepository ingredientRepository;
    private final ImageAssetUrlResolver imageAssetUrlResolver;

    @Override
    public List<IngredientDto.SummaryResponse> list(Query query) {
        return ingredientRepository.findAllByUserIdAndStatus(query.userId(), IngredientStatus.ACTIVE).stream()
                .map(ingredient -> IngredientDto.SummaryResponse.from(ingredient, imageAssetUrlResolver))
                .toList();
    }
}
