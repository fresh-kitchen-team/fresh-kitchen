package com.example.freshkitchen.application.ingredient.service;

import com.example.freshkitchen.application.ingredient.dto.IngredientDto;
import com.example.freshkitchen.application.ingredient.usecase.ListIngredientsUseCase;
import com.example.freshkitchen.application.image.port.ImageAssetUrlResolver;
import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import com.example.freshkitchen.domain.ingredient.enums.IngredientStatus;
import com.example.freshkitchen.domain.ingredient.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListIngredientsService implements ListIngredientsUseCase {

    private final IngredientRepository ingredientRepository;
    private final ImageAssetUrlResolver imageAssetUrlResolver;
    private final Clock clock;

    @Override
    public List<IngredientDto.SummaryResponse> list(Query query) {
        Stream<Ingredient> stream = ingredientRepository
                .findAllByUserIdAndStatus(query.userId(), IngredientStatus.ACTIVE)
                .stream();

        if (query.storageType() != null) {
            stream = stream.filter(i -> i.getStorage().getStorageType() == query.storageType());
        }

        if (query.maxDDay() != null) {
            LocalDate today = LocalDate.now(clock);
            LocalDate deadline = today.plusDays(query.maxDDay());
            stream = stream.filter(i -> i.getExpiresAt() != null && !i.getExpiresAt().isAfter(deadline));
        }

        stream = stream.sorted(Comparator.comparing(
                Ingredient::getExpiresAt,
                Comparator.nullsLast(Comparator.naturalOrder())
        ));

        return stream
                .map(ingredient -> IngredientDto.SummaryResponse.from(ingredient, imageAssetUrlResolver))
                .toList();
    }
}
