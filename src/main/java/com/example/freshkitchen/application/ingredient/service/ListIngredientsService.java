package com.example.freshkitchen.application.ingredient.service;

import com.example.freshkitchen.application.ingredient.dto.IngredientSummaryResult;
import com.example.freshkitchen.application.ingredient.usecase.ListIngredientsUseCase;
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

    @Override
    public List<IngredientSummaryResult> list(Query query) {
        return ingredientRepository.findAllByUserId(query.userId()).stream()
                .map(IngredientSummaryResult::from)
                .toList();
    }
}
