package com.example.freshkitchen.application.ingredient.service;

import com.example.freshkitchen.application.ingredient.dto.IngredientDto;
import com.example.freshkitchen.application.ingredient.usecase.ListIngredientsUseCase;
import com.example.freshkitchen.domain.ingredient.repository.IngredientRepository;
import com.example.freshkitchen.domain.ingredient.repository.IngredientRepositoryImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListIngredientsService implements ListIngredientsUseCase {

    private final IngredientRepositoryImpl ingredientRepository;

    @Override
    public List<IngredientDto.SummaryResponse> list(Query query) {
        return ingredientRepository.findAllByUserId(query.userId()).stream()
                .map(IngredientDto.SummaryResponse::from)
                .toList();
    }
}
