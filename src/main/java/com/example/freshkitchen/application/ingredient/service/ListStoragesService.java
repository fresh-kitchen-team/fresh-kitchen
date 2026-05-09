package com.example.freshkitchen.application.ingredient.service;

import com.example.freshkitchen.application.ingredient.dto.IngredientDto;
import com.example.freshkitchen.application.ingredient.usecase.ListStoragesUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ListStoragesService implements ListStoragesUseCase {

    private final DefaultStorageService defaultStorageService;

    @Override
    public List<IngredientDto.StorageSummaryResponse> list(Query query) {
        return defaultStorageService.ensureDefaultStorages(query.userId()).stream()
                .map(IngredientDto.StorageSummaryResponse::from)
                .toList();
    }
}
